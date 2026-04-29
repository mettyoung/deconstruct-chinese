package com.mettyoung.deconstructchinese.model

import kotlinx.serialization.Serializable

@Serializable
enum class Language(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    CHINESE_TRADITIONAL("Traditional Chinese", "zh-TW"),
    CHINESE_SIMPLIFIED("Simplified Chinese", "zh-CN"),
    JAPANESE("Japanese", "ja"),
    KOREAN("Korean", "ko"),
    FRENCH("French", "fr"),
    SPANISH("Spanish", "es"),
    GERMAN("German", "de")
}

@Serializable
data class VocabularyItem(
    val word: String,
    val phonetic: String,
    val meaning: String
)

@Serializable
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val phoneticText: String,
    val vocabulary: List<VocabularyItem>,
    val grammarNote: String = "",
    val sourceLanguage: Language,
    val targetLanguage: Language
)

sealed class TranslationState {
    data object Idle : TranslationState()
    data object Loading : TranslationState()
    data class Success(val result: TranslationResult) : TranslationState()
    data class Error(val message: String) : TranslationState()
}
