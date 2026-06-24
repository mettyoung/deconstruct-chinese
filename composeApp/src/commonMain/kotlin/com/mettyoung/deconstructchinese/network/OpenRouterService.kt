package com.mettyoung.deconstructchinese.network

class OpenRouterService(
    apiKey: String,
    model: String = DEFAULT_MODEL,
    baseUrl: String = DEFAULT_BASE_URL
) : OpenAiCompatibleTranslator(
    apiKey = apiKey,
    baseUrl = baseUrl,
    model = model,
    providerLabel = "OpenRouter",
    userPromptPrefix = "/no_think"
) {
    companion object {
        const val DEFAULT_MODEL = "qwen/qwen3-14b"
        const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
    }
}
