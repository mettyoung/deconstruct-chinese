package com.mettyoung.deconstructchinese.network

import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.VocabularyItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.String

// ── Qwen API wire format ─────────────────────────────────────────────

@Serializable
data class QwenRequest(
    val model: String = "qwen-plus",
    val messages: List<QwenMessage>,
    val parameters: QwenParameters? = null
)

@Serializable
data class QwenMessage(
    val role: String,
    val content: String
)

@Serializable
data class QwenParameters(
    val result_format: String = "message"
)

@Serializable
data class QwenResponse(
    val choices: List<QwenChoice>? = null
)

@Serializable
data class QwenChoice(
    val message: QwenMessage? = null,
    val finish_reason: String? = null
)

// ── The service class ──────────────────────────────────────────────────

class QwenService(private val apiKey: String) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    private val baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions"

    suspend fun translate(englishText: String): TranslationResult {
        val systemPrompt = "You are a Chinese language teacher. Translate to Traditional Chinese. Respond ONLY with valid JSON."
        val userPrompt = buildPrompt(englishText)
        
        val requestBody = QwenRequest(
            messages = listOf(
                QwenMessage("system", systemPrompt),
                QwenMessage("user", userPrompt)
            ),
            parameters = QwenParameters(result_format = "message")
        )

        // Log the curl command
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

        return parseQwenResponse(rawText, englishText)
    }

    private fun logCurl(requestBody: QwenRequest) {
        val json = Json { prettyPrint = true }
        val bodyString = json.encodeToString(requestBody)
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

    private fun buildPrompt(englishText: String): String {
        return """
Translate the following English text to Traditional Chinese.
English: "$englishText"

Return this exact JSON structure:
{
  "chineseText": "the full Chinese translation",
  "pinyinText": "full pinyin with tone marks for the whole sentence",
  "grammarNote": "one sentence explaining the grammar structure in english",
  "vocabulary": [
    {
      "character": "Chinese character or word",
      "pinyin": "pinyin with tone marks",
      "meaning": "English meaning"
    }
  ]
}

Rules:
- Use proper tone marks in pinyin (ā á ǎ à etc.)
- Keep grammarNote short and educational
- Return ONLY the JSON, nothing else
        """.trimIndent()
    }

    private fun parseQwenResponse(
        rawText: String,
        originalText: String
    ): TranslationResult {
        val cleanJson = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = Json { ignoreUnknownKeys = true; isLenient = true }

        @Serializable
        data class VocabDto(
            val character: String,
            val pinyin: String,
            val meaning: String
        )

        @Serializable
        data class QwenTranslation(
            val chineseText: String,
            val pinyinText: String,
            val grammarNote: String = "",
            val vocabulary: List<VocabDto>
        )

        val parsed = json.decodeFromString<QwenTranslation>(cleanJson)

        return TranslationResult(
            originalText = originalText,
            chineseText = parsed.chineseText,
            pinyinText = parsed.pinyinText,
            grammarNote = parsed.grammarNote,
            vocabulary = parsed.vocabulary.map {
                VocabularyItem(it.character, it.pinyin, it.meaning)
            }
        )
    }
}
