package com.mettyoung.deconstructchinese.network

import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.VocabularyItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Gemini API wire format ─────────────────────────────────────────────
// These mirror the exact JSON structure Gemini's REST API uses

@Serializable
data class GeminiRequest(val contents: List<Content>)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String)

@Serializable
data class GeminiResponse(val candidates: List<Candidate>? = null)

@Serializable
data class Candidate(val content: Content? = null)

// ── The service class ──────────────────────────────────────────────────

class GeminiService(private val apiKey: String) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    suspend fun translate(englishText: String): TranslationResult {
        val prompt = buildPrompt(englishText)

        val response: GeminiResponse = client.post(baseUrl) {
            url { parameters.append("key", apiKey) }
            contentType(ContentType.Application.Json)
            setBody(GeminiRequest(listOf(Content(listOf(Part(prompt))))))
        }.body()

        val rawText = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: throw Exception("Empty response from Gemini")

        return parseGeminiResponse(rawText, englishText)
    }

    private fun buildPrompt(englishText: String): String {
        return """
You are a Chinese language teacher. Translate the following English text to Mandarin Chinese.
Respond ONLY with a valid JSON object, no markdown, no extra text.

English: "$englishText"

Return this exact JSON structure:
{
  "chineseText": "the full Chinese translation",
  "pinyinText": "full pinyin with tone marks for the whole sentence",
  "grammarNote": "one sentence explaining the grammar structure",
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

    private fun parseGeminiResponse(
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
        data class GeminiTranslation(
            val chineseText: String,
            val pinyinText: String,
            val grammarNote: String = "",
            val vocabulary: List<VocabDto>
        )

        val parsed = json.decodeFromString<GeminiTranslation>(cleanJson)

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