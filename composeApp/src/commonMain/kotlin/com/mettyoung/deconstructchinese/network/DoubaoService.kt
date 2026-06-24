package com.mettyoung.deconstructchinese.network

class DoubaoService(
    apiKey: String,
    model: String = DEFAULT_MODEL,
    baseUrl: String = DEFAULT_BASE_URL
) : OpenAiCompatibleTranslator(
    apiKey = apiKey,
    baseUrl = baseUrl,
    model = model,
    providerLabel = "Doubao",
    // seed-2-0-lite is a hybrid reasoning model; skip the chain-of-thought.
    disableThinking = true
) {
    companion object {
        const val DEFAULT_MODEL = "seed-2-0-lite-260228"
        const val DEFAULT_BASE_URL = "https://ark.ap-southeast.bytepluses.com/api/v3/chat/completions"
    }
}
