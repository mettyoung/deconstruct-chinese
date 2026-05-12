package com.mettyoung.deconstructchinese.network

import com.mettyoung.deconstructchinese.model.Language
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.VocabularyItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class QwenRequest(
    val model: String = "qwen-plus",
    val messages: List<QwenMessage>,
    val temperature: Double = 0.0
)

@Serializable
data class QwenMessage(
    val role: String,
    val content: String
)

@Serializable
data class QwenResponse(
    val choices: List<QwenChoice>? = null,
    val id: String? = null
)

@Serializable
data class QwenChoice(
    val message: QwenMessage? = null,
    val finish_reason: String? = null
)

class QwenService(private val apiKey: String) {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true 
    }

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    private val baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions"

    suspend fun translate(text: String, toEnglish: Boolean = false, useSimplified: Boolean = true): TranslationResult {
        val chineseVariant = if (useSimplified) "Simplified Chinese (简体中文)" else "Traditional Chinese (繁體中文)"
        val systemPrompt = if (toEnglish)
            "You are a professional Chinese language teacher and translator. Translate Chinese text into English, and provide a detailed Chinese vocabulary breakdown. Respond ONLY with valid JSON."
        else
            "You are a professional translator and language teacher specializing in $chineseVariant. Translate English into $chineseVariant and provide a vocabulary breakdown. Respond ONLY with valid JSON."
        val userPrompt = if (toEnglish) buildPromptToEnglish(text, useSimplified) else buildPromptToChinese(text, useSimplified)
        
        val requestBody = QwenRequest(
            messages = listOf(
                QwenMessage("system", systemPrompt),
                QwenMessage("user", userPrompt)
            )
        )

        logCurl(requestBody)

        val response: HttpResponse = client.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("Qwen API error: ${response.status} - $errorBody")
        }

        val body: QwenResponse = response.body()

        val rawText = body.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?: throw Exception("Empty response from Qwen")

        return parseQwenResponse(rawText, text, toEnglish, useSimplified)
    }

    private fun logCurl(requestBody: QwenRequest) {
        val bodyString = jsonConfig.encodeToString(requestBody)
        val curl = """
            curl "$baseUrl" \
            -H "Authorization: Bearer $apiKey" \
            -H "Content-Type: application/json" \
            -d '${bodyString.replace("'", "\\'")}'
        """.trimIndent()
        
        println("── DEBUG: Qwen API cURL ──────────────────────────────────────")
        println(curl)
        println("────────────────────────────────────────────────────────────────")
    }

    private fun buildPromptToChinese(text: String, useSimplified: Boolean): String {
        val variant = if (useSimplified) "Simplified Chinese (简体中文)" else "Traditional Chinese (繁體中文)"
        val scriptRule = if (useSimplified)
            "translatedText must use Simplified Chinese characters (简体中文), never Traditional."
        else
            "translatedText must use Traditional Chinese characters (繁體中文), never Simplified."
        return """
Translate the following English text into $variant.

Input: "$text"

Return this exact JSON:
{
  "translatedText": "the full translation in $variant",
  "phoneticText": "pinyin with tone marks for the entire translatedText",
  "grammarNote": "one sentence in English describing the Chinese sentence structure and grammar used",
  "vocabulary": [
    {
      "word": "the Traditional Chinese form of this word",
      "simplified": "the Simplified Chinese form — omit this field only if traditional and simplified are identical",
      "phonetic": "pinyin with tone marks for this word",
      "meaning": "English meaning of this word"
    }
  ]
}

Rules:
- $scriptRule
- phoneticText and every vocabulary phonetic must be pinyin with tone marks.
- grammarNote must be in English, describing the grammar of the Chinese output.
- vocabulary must segment translatedText into natural words, not individual characters. Multi-character words must appear as a single vocabulary entry. Do not split compound words.
- vocabulary covers every word in translatedText in order — do not skip any.
- word is ALWAYS the Traditional Chinese form regardless of the preferred script. simplified is ALWAYS the Simplified Chinese form, omitted only when the characters are identical.
- Return ONLY the JSON, nothing else.
        """.trimIndent()
    }

    private fun buildPromptToEnglish(text: String, useSimplified: Boolean): String {
        val variant = if (useSimplified) "Simplified Chinese (简体中文)" else "Traditional Chinese (繁體中文)"
        return """
Translate the following Chinese text into English. The input may be Traditional Chinese, Simplified Chinese, or a mix.

Input: "$text"

Return this exact JSON:
{
  "traditionalChineseText": "the input normalized to $variant",
  "translatedText": "the full translation in English",
  "phoneticText": "pinyin with tone marks for traditionalChineseText",
  "grammarNote": "one sentence in English describing the Chinese sentence structure and grammar",
  "vocabulary": [
    {
      "word": "the Traditional Chinese form of this word",
      "simplified": "the Simplified Chinese form — omit this field only if traditional and simplified are identical",
      "phonetic": "pinyin with tone marks for this word",
      "meaning": "English meaning of this word"
    }
  ]
}

Rules:
- traditionalChineseText must use $variant characters.
- phoneticText is the pinyin of traditionalChineseText, not the English translation.
- grammarNote must be in English, describing the grammar of the Chinese input.
- vocabulary must segment traditionalChineseText into natural words, not individual characters. Multi-character words must appear as a single vocabulary entry. Do not split compound words.
- vocabulary covers every word in traditionalChineseText in order — do not skip any.
- word is ALWAYS the Traditional Chinese form regardless of the preferred script. simplified is ALWAYS the Simplified Chinese form, omitted only when the characters are identical.
- Return ONLY the JSON, nothing else.
        """.trimIndent()
    }

    private fun parseQwenResponse(rawText: String, originalText: String, toEnglish: Boolean, useSimplified: Boolean): TranslationResult {
        val cleanJson = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        @Serializable
        data class VocabDto(
            val word: String,
            val simplified: String? = null,
            val phonetic: String,
            val meaning: String
        )

        @Serializable
        data class QwenTranslation(
            val traditionalChineseText: String = "",
            val translatedText: String,
            val phoneticText: String,
            val grammarNote: String = "",
            val vocabulary: List<VocabDto>
        )

        val parsed = jsonConfig.decodeFromString<QwenTranslation>(cleanJson)
        val chineseText = if (toEnglish) parsed.traditionalChineseText else parsed.translatedText
        val chineseLang = if (useSimplified) Language.CHINESE_SIMPLIFIED else Language.CHINESE_TRADITIONAL

        return TranslationResult(
            originalText   = originalText,
            translatedText = parsed.translatedText,
            chineseText    = chineseText,
            phoneticText   = parsed.phoneticText,
            grammarNote    = parsed.grammarNote,
            vocabulary     = parsed.vocabulary.map { VocabularyItem(it.word, it.phonetic, it.meaning, simplified = it.simplified) },
            sourceLanguage = if (toEnglish) chineseLang else Language.ENGLISH,
            targetLanguage = if (toEnglish) Language.ENGLISH else chineseLang
        )
    }
}
