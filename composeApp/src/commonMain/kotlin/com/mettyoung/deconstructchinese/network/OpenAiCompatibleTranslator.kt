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
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.0,
    val response_format: ResponseFormat? = null,
    val stream: Boolean = false,
    val thinking: Thinking? = null
)

@Serializable
private data class Thinking(val type: String)

@Serializable
private data class StreamChunk(
    val choices: List<StreamChoice>? = null
)

@Serializable
private data class StreamChoice(
    val delta: StreamDelta? = null
)

@Serializable
private data class StreamDelta(
    val content: String? = null
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
    private val useJsonMode: Boolean = true,
    private val userPromptPrefix: String = "",
    // Doubao's seed models are hybrid reasoning models that stream a
    // chain-of-thought before the answer — the dominant latency cost. Adapters
    // that hit such a model set this true to request a direct (non-thinking) reply.
    private val disableThinking: Boolean = false
) : TranslationService {

    private val thinkingMode: Thinking? =
        if (disableThinking) Thinking("disabled") else null

    final override suspend fun translate(
        text: String,
        toEnglish: Boolean,
        useSimplified: Boolean,
        includeGrammarNote: Boolean
    ): TranslationResult {
        val t0 = currentTimeMillis()
        println("[TranslationService] start provider=$providerLabel model=$model url=$baseUrl includeGrammarNote=$includeGrammarNote jsonMode=$useJsonMode chars=${text.length}")
        val systemPrompt = if (toEnglish) SYSTEM_TO_EN else systemToChinese(useSimplified)
        val baseUserPrompt = if (toEnglish) buildPromptToEnglish(text, useSimplified, includeGrammarNote)
        else buildPromptToChinese(text, useSimplified, includeGrammarNote)
        val userPrompt = if (userPromptPrefix.isNotEmpty()) "$userPromptPrefix\n$baseUserPrompt" else baseUserPrompt

        val requestBody = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            response_format = if (useJsonMode) ResponseFormat("json_object") else null,
            thinking = thinkingMode
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

    final override fun translateStream(
        text: String,
        toEnglish: Boolean,
        useSimplified: Boolean
    ): Flow<String> = flow {
        val t0 = currentTimeMillis()
        println("[TranslationService] stream start provider=$providerLabel model=$model chars=${text.length}")
        val systemPrompt = STREAM_SYSTEM
        val userPrompt = buildStreamPrompt(text, toEnglish, useSimplified)

        val requestBody = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            stream = true,
            thinking = thinkingMode
        )

        sharedClient.preparePost(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw Exception("$providerLabel API error: ${response.status} - $errorBody")
            }
            val channel = response.bodyAsChannel()
            val acc = StringBuilder()
            var firstToken = true
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty()) continue
                if (payload == "[DONE]") break
                val delta = runCatching {
                    jsonConfig.decodeFromString<StreamChunk>(payload)
                        .choices?.firstOrNull()?.delta?.content
                }.getOrNull() ?: continue
                if (delta.isEmpty()) continue
                if (firstToken) {
                    println("[TranslationService] stream first-token=${currentTimeMillis() - t0}ms")
                    firstToken = false
                }
                acc.append(delta)
                emit(acc.toString())
            }
        }
        println("[TranslationService] stream done provider=$providerLabel total=${currentTimeMillis() - t0}ms")
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

        // Stage 1: tiny output for fast first paint. No JSON, no vocabulary.
        private const val STREAM_SYSTEM =
            "You are a translator. Output ONLY the translation text — no quotes, " +
                "no pinyin, no explanations, no extra words."

        private fun buildStreamPrompt(
            text: String,
            toEnglish: Boolean,
            useSimplified: Boolean
        ): String = if (toEnglish) {
            "Translate to English:\n$text"
        } else {
            val variant = if (useSimplified) SIMPLIFIED else TRADITIONAL
            "Translate to $variant:\n$text"
        }
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
                    requestTimeoutMillis = 120_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 120_000
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
