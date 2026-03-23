package com.mettyoung.deconstructchinese.model

import kotlinx.serialization.Serializable

@Serializable
data class VocabularyItem(
    val character: String,
    val pinyin: String,
    val meaning: String
)

@Serializable
data class TranslationResult(
    val originalText: String,
    val chineseText: String,
    val pinyinText: String,
    val vocabulary: List<VocabularyItem>,
    val grammarNote: String = ""
)

sealed class TranslationState {
    object Idle : TranslationState()
    object Loading : TranslationState()
    data class Success(val result: TranslationResult) : TranslationState()
    data class Error(val message: String) : TranslationState()
}