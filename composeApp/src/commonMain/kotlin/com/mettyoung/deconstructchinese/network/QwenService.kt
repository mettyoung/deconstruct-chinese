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

    suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): TranslationResult {
        val systemPrompt = "You are a professional translator and language teacher. Translate from ${sourceLanguage.displayName} to ${targetLanguage.displayName}. Respond ONLY with valid JSON."
        val userPrompt = buildPrompt(text, sourceLanguage, targetLanguage)
        
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

        return parseQwenResponse(rawText, text, sourceLanguage, targetLanguage)
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

    private fun buildPrompt(text: String, sourceLanguage: Language, targetLanguage: Language): String {
        return """
Translate the following text from ${sourceLanguage.displayName} to ${targetLanguage.displayName}.
Text: "$text"

Return this exact JSON structure:
{
  "translatedText": "the full translation in ${targetLanguage.displayName}",
  "phoneticText": "phonetic transcription (like pinyin for Chinese, furigana for Japanese, etc.)",
  "grammarNote": "one sentence explaining the grammar structure in ${sourceLanguage.displayName}",
  "vocabulary": [
    {
      "word": "every individual word or token from the translated text",
      "phonetic": "phonetic transcription of this word",
      "meaning": "meaning in ${sourceLanguage.displayName}"
    }
  ]
}

Rules:
- The "vocabulary" array MUST contain an entry for EVERY word or token that appears in the translated text — do not skip any word.
- List vocabulary entries in the same order the words appear in the translated text.
- If the target language doesn't typically use phonetic transcription (like English or Spanish), leave "phoneticText" and "phonetic" empty or use standard IPA.
- Return ONLY the JSON, nothing else.
        """.trimIndent()
    }

    private fun parseQwenResponse(
        rawText: String,
        originalText: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): TranslationResult {
        val cleanJson = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        @Serializable
        data class VocabDto(
            val word: String,
            val phonetic: String,
            val meaning: String
        )

        @Serializable
        data class QwenTranslation(
            val translatedText: String,
            val phoneticText: String,
            val grammarNote: String = "",
            val vocabulary: List<VocabDto>
        )

        val parsed = jsonConfig.decodeFromString<QwenTranslation>(cleanJson)

        return TranslationResult(
            originalText = originalText,
            translatedText = parsed.translatedText,
            phoneticText = parsed.phoneticText,
            grammarNote = parsed.grammarNote,
            vocabulary = parsed.vocabulary.map {
                VocabularyItem(it.word, it.phonetic, it.meaning)
            },
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
    }
}
