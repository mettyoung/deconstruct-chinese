package com.mettyoung.deconstructchinese.network

@Suppress("unused")
class QwenService(
    apiKey: String,
    model: String = DEFAULT_MODEL,
    baseUrl: String = DEFAULT_BASE_URL
) : OpenAiCompatibleTranslator(
    apiKey = apiKey,
    baseUrl = baseUrl,
    model = model,
    providerLabel = "Qwen"
) {
    companion object {
        const val DEFAULT_MODEL = "qwen-plus"
        const val DEFAULT_BASE_URL = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions"
    }
}
