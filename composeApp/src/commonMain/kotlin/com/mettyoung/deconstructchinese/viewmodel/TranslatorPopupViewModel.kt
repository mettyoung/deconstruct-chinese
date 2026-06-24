package com.mettyoung.deconstructchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mettyoung.deconstructchinese.model.Language
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.network.TranslationService
import com.mettyoung.deconstructchinese.storage.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TranslatorPopupViewModel(
    private val translationService: TranslationService,
    private val apiKey: String,
    private val useSimplified: Boolean = AppSettings.useSimplified
) : ViewModel() {

    private val _translationState =
        MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    fun translate(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _translationState.value = TranslationState.Idle
            return
        }
        if (!containsHan(trimmed)) {
            _translationState.value = TranslationState.Error(NOT_CHINESE_MESSAGE)
            return
        }
        if (apiKey.isBlank()) {
            _translationState.value = TranslationState.Error(MISSING_API_KEY_MESSAGE)
            return
        }
        viewModelScope.launch {
            _translationState.value = TranslationState.Loading
            val chineseLang =
                if (useSimplified) Language.CHINESE_SIMPLIFIED else Language.CHINESE_TRADITIONAL
            try {
                // Stage 1: stream the plain translation for fast first paint.
                translationService.translateStream(
                    text = trimmed,
                    toEnglish = true,
                    useSimplified = useSimplified
                ).collect { acc ->
                    _translationState.value = TranslationState.Success(
                        result = TranslationResult(
                            originalText = trimmed,
                            translatedText = acc,
                            chineseText = trimmed,
                            phoneticText = "",
                            vocabulary = emptyList(),
                            sourceLanguage = chineseLang,
                            targetLanguage = Language.ENGLISH
                        ),
                        vocabLoading = true
                    )
                }

                // Stage 2: full breakdown.
                val result = translationService.translate(
                    text = trimmed,
                    toEnglish = true,
                    useSimplified = useSimplified,
                    includeGrammarNote = false
                )
                _translationState.value = TranslationState.Success(result, vocabLoading = false)
            } catch (e: Exception) {
                val current = _translationState.value
                if (current is TranslationState.Success) {
                    // Keep the streamed translation; only the breakdown failed.
                    _translationState.value = current.copy(vocabLoading = false)
                } else {
                    _translationState.value = TranslationState.Error(
                        when {
                            e.message?.contains("401") == true ->
                                "Invalid API key. Please check your API key."
                            e.message?.contains("429") == true ->
                                "Rate limit reached. Wait a moment and try again."
                            e.message?.contains("connect") == true ->
                                "Network error. Check your internet connection."
                            else -> "Translation failed: ${e.message}"
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val NOT_CHINESE_MESSAGE = "Deconstruct Chinese only translates Chinese text."
        const val MISSING_API_KEY_MESSAGE = "Set your API key in the app to translate."

        fun containsHan(text: String): Boolean =
            text.any { it.code in 0x4E00..0x9FFF }
    }
}
