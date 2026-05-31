package com.mettyoung.deconstructchinese.network

import com.mettyoung.deconstructchinese.model.Language
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.VocabularyItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.0,
    val response_format: ResponseFormat? = null
)

@Serializable
private data class ResponseFormat(val type: String)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
private data class ChatResponse(
    val choices: List<ChatChoice>? = null,
    val id: String? = null
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage? = null,
    val finish_reason: String? = null
)

abstract class OpenAiCompatibleTranslator(
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String,
    private val providerLabel: String,
    private val useJsonMode: Boolean = true
) : TranslationService {

    final override suspend fun translate(
        text: String,
        toEnglish: Boolean,
        useSimplified: Boolean,
        includeGrammarNote: Boolean
    ): TranslationResult {
        val t0 = currentTimeMillis()
        println("[TranslationService] start provider=$providerLabel model=$model url=$baseUrl includeGrammarNote=$includeGrammarNote jsonMode=$useJsonMode chars=${text.length}")
        val systemPrompt = if (toEnglish) SYSTEM_TO_EN else systemToChinese(useSimplified)
        val userPrompt = if (toEnglish) buildPromptToEnglish(text, useSimplified, includeGrammarNote)
        else buildPromptToChinese(text, useSimplified, includeGrammarNote)

        val requestBody = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            response_format = if (useJsonMode) ResponseFormat("json_object") else null
        )

        logCurl(requestBody)

        val tSend = currentTimeMillis()
        val response: HttpResponse = sharedClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        val tHeaders = currentTimeMillis()

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("$providerLabel API error: ${response.status} - $errorBody")
        }

        val body: ChatResponse = response.body()
        val tBody = currentTimeMillis()
        val rawText = body.choices?.firstOrNull()?.message?.content
            ?: throw Exception("Empty response from $providerLabel")

        val result = parseResponse(rawText, text, toEnglish, useSimplified)
        val tDone = currentTimeMillis()
        println("[TranslationService] done provider=$providerLabel total=${tDone - t0}ms send=${tSend - t0}ms headers=${tHeaders - tSend}ms body=${tBody - tHeaders}ms parse=${tDone - tBody}ms outChars=${rawText.length}")
        return result
    }

    private fun currentTimeMillis(): Long = io.ktor.util.date.getTimeMillis()

    private fun logCurl(requestBody: ChatRequest) {
        val bodyString = jsonConfig.encodeToString(requestBody)
        val curl = """
            curl "$baseUrl" \
            -H "Authorization: Bearer $apiKey" \
            -H "Content-Type: application/json" \
            -d '${bodyString.replace("'", "\\'")}'
        """.trimIndent()

        println("── DEBUG: $providerLabel API cURL ──────────────────────────────")
        println(curl)
        println("────────────────────────────────────────────────────────────────")
    }

    private fun parseResponse(
        rawText: String,
        originalText: String,
        toEnglish: Boolean,
        useSimplified: Boolean
    ): TranslationResult {
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
        data class TranslationDto(
            val traditionalChineseText: String = "",
            val translatedText: String,
            val phoneticText: String,
            val grammarNote: String = "",
            val vocabulary: List<VocabDto>
        )

        val parsed = jsonConfig.decodeFromString<TranslationDto>(cleanJson)
        val chineseText = if (toEnglish) parsed.traditionalChineseText else parsed.translatedText
        val chineseLang = if (useSimplified) Language.CHINESE_SIMPLIFIED else Language.CHINESE_TRADITIONAL

        return TranslationResult(
            originalText = originalText,
            translatedText = parsed.translatedText,
            chineseText = chineseText,
            phoneticText = parsed.phoneticText,
            grammarNote = parsed.grammarNote,
            vocabulary = parsed.vocabulary.map {
                VocabularyItem(it.word, it.phonetic, it.meaning, simplified = it.simplified)
            },
            sourceLanguage = if (toEnglish) chineseLang else Language.ENGLISH,
            targetLanguage = if (toEnglish) Language.ENGLISH else chineseLang
        )
    }

    companion object {
        private const val SIMPLIFIED = "Simplified Chinese (简体中文)"
        private const val TRADITIONAL = "Traditional Chinese (繁體中文)"
        private const val SYSTEM_TO_EN =
            "You are a professional Chinese language teacher and translator. " +
                "Translate Chinese text into English, and provide a detailed Chinese vocabulary breakdown. " +
                "Respond ONLY with valid JSON."

        private val jsonConfig = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }

        // One HttpClient for the whole process: connection pool + TLS session
        // survive across popup launches and avoid a fresh handshake per call.
        private val sharedClient: HttpClient by lazy {
            HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 60_000
                }
                install(ContentNegotiation) {
                    json(jsonConfig)
                }
                install(Logging) {
                    level = LogLevel.INFO
                }
            }
        }

        private fun systemToChinese(useSimplified: Boolean): String {
            val v = if (useSimplified) SIMPLIFIED else TRADITIONAL
            return "You are a professional translator and language teacher specializing in $v. " +
                "Translate English into $v and provide a vocabulary breakdown. Respond ONLY with valid JSON."
        }

        private fun buildPromptToChinese(
            text: String,
            useSimplified: Boolean,
            includeGrammarNote: Boolean
        ): String {
            val variant = if (useSimplified) SIMPLIFIED else TRADITIONAL
            val scriptRule = if (useSimplified)
                "translatedText must use Simplified Chinese characters (简体中文), never Traditional."
            else
                "translatedText must use Traditional Chinese characters (繁體中文), never Simplified."
            val grammarField = if (includeGrammarNote)
                "\"grammarNote\": \"one sentence in English describing the Chinese sentence structure and grammar used\",\n  "
            else ""
            val grammarRule = if (includeGrammarNote)
                "- grammarNote must be in English, describing the grammar of the Chinese output.\n"
            else ""
            return """
Translate the following English text into $variant.

Input: "$text"

Return this exact JSON:
{
  "translatedText": "the full translation in $variant",
  "phoneticText": "pinyin with tone marks for the entire translatedText",
  $grammarField"vocabulary": [
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
$grammarRule- vocabulary must segment translatedText into natural words, not individual characters. Multi-character words must appear as a single vocabulary entry. Do not split compound words.
- vocabulary covers every word in translatedText in order — do not skip any.
- word is ALWAYS the Traditional Chinese form regardless of the preferred script. simplified is ALWAYS the Simplified Chinese form, omitted only when the characters are identical.
- Return ONLY the JSON, nothing else.
            """.trimIndent()
        }

        private fun buildPromptToEnglish(
            text: String,
            useSimplified: Boolean,
            includeGrammarNote: Boolean
        ): String {
            val variant = if (useSimplified) SIMPLIFIED else TRADITIONAL
            val grammarField = if (includeGrammarNote)
                "\"grammarNote\": \"one sentence in English describing the Chinese sentence structure and grammar\",\n  "
            else ""
            val grammarRule = if (includeGrammarNote)
                "- grammarNote must be in English, describing the grammar of the Chinese input.\n"
            else ""
            return """
Translate the following Chinese text into English. The input may be Traditional Chinese, Simplified Chinese, or a mix.

Input: "$text"

Return this exact JSON:
{
  "traditionalChineseText": "the input normalized to $variant",
  "translatedText": "the full translation in English",
  "phoneticText": "pinyin with tone marks for traditionalChineseText",
  $grammarField"vocabulary": [
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
$grammarRule- vocabulary must segment traditionalChineseText into natural words, not individual characters. Multi-character words must appear as a single vocabulary entry. Do not split compound words.
- vocabulary covers every word in traditionalChineseText in order — do not skip any.
- word is ALWAYS the Traditional Chinese form regardless of the preferred script. simplified is ALWAYS the Simplified Chinese form, omitted only when the characters are identical.
- Return ONLY the JSON, nothing else.
            """.trimIndent()
        }
    }
}
