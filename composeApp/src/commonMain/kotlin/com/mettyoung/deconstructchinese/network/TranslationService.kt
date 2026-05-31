package com.mettyoung.deconstructchinese.network

import com.mettyoung.deconstructchinese.model.TranslationResult

interface TranslationService {
    suspend fun translate(
        text: String,
        toEnglish: Boolean = false,
        useSimplified: Boolean = true,
        includeGrammarNote: Boolean = true
    ): TranslationResult
}
