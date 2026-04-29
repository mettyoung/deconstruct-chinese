package com.mettyoung.deconstructchinese.network

import com.mettyoung.deconstructchinese.model.Language
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.VocabularyItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
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
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"

    suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): TranslationResult {
        val prompt = buildPrompt(text, sourceLanguage, targetLanguage)

        val response: HttpResponse = client.post(baseUrl) {
            url { parameters.append("key", apiKey) }
            contentType(ContentType.Application.Json)
            setBody(GeminiRequest(listOf(Content(listOf(Part(prompt))))))
        }

        if (!response.status.isSuccess()) {
            throw Exception("Gemini API error: ${response.status}")
        }

        val body: GeminiResponse = response.body()

        val rawText = body.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: throw Exception("Empty response from Gemini")

        return parseGeminiResponse(rawText, text, sourceLanguage, targetLanguage)
    }

    private fun buildPrompt(text: String, sourceLanguage: Language, targetLanguage: Language): String {
        return """
You are a professional translator and language teacher. Translate from ${sourceLanguage.displayName} to ${targetLanguage.displayName}.
Respond ONLY with a valid JSON object, no markdown, no extra text.

Text: "$text"

Return this exact JSON structure:
{
  "translatedText": "the full translation",
  "phoneticText": "phonetic transcription (like pinyin for Chinese, furigana for Japanese, etc.)",
  "grammarNote": "one sentence explaining the grammar structure",
  "vocabulary": [
    {
      "word": "every individual word or token from the translated text",
      "phonetic": "phonetic transcription",
      "meaning": "meaning in ${sourceLanguage.displayName}"
    }
  ]
}

Rules:
- The "vocabulary" array MUST contain an entry for EVERY word or token that appears in the translated text — do not skip any word.
- List vocabulary entries in the same order the words appear in the translated text.
- If the target language doesn't typically use phonetic transcription (like English or Spanish), leave "phoneticText" and "phonetic" empty or use standard IPA.
- Return ONLY the JSON, nothing else
        """.trimIndent()
    }

    private fun parseGeminiResponse(
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

        val json = Json { ignoreUnknownKeys = true; isLenient = true }

        @Serializable
        data class VocabDto(
            val word: String,
            val phonetic: String,
            val meaning: String
        )

        @Serializable
        data class GeminiTranslation(
            val translatedText: String,
            val phoneticText: String,
            val grammarNote: String = "",
            val vocabulary: List<VocabDto>
        )

        val parsed = json.decodeFromString<GeminiTranslation>(cleanJson)

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
