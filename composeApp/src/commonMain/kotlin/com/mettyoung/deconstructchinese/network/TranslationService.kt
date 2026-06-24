package com.mettyoung.deconstructchinese.network

import com.mettyoung.deconstructchinese.model.TranslationResult
import kotlinx.coroutines.flow.Flow

interface TranslationService {
    suspend fun translate(
        text: String,
        toEnglish: Boolean = false,
        useSimplified: Boolean = true,
        includeGrammarNote: Boolean = true
    ): TranslationResult

    /**
     * Stage 1: stream a plain translation (no JSON, no vocabulary) for fast
     * first paint. Emits the accumulated translation text as tokens arrive.
     */
    fun translateStream(
        text: String,
        toEnglish: Boolean = false,
        useSimplified: Boolean = true
    ): Flow<String>
}
