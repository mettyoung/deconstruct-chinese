package com.mettyoung.deconstructchinese.storage

import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
object VocabularyStore {
    private val settings: Settings = Settings()
    private const val KEY_VOCABULARY = "saved_vocabulary"

    private val _savedVocabulary = MutableStateFlow<List<VocabularyItem>>(loadVocabulary())
    val savedVocabulary: StateFlow<List<VocabularyItem>> = _savedVocabulary.asStateFlow()

    private fun loadVocabulary(): List<VocabularyItem> {
        return (settings.decodeValueOrNull<List<VocabularyItem>>(KEY_VOCABULARY) ?: emptyList())
            .sortedByDescending { it.frequency }
    }

    private fun saveToSettings(list: List<VocabularyItem>) {
        settings.encodeValue(KEY_VOCABULARY, list)
    }

    private fun persist(list: List<VocabularyItem>) {
        val sorted = list.sortedByDescending { it.frequency }
        _savedVocabulary.value = sorted
        saveToSettings(sorted)
    }

    fun saveWord(item: VocabularyItem) {
        val currentList = _savedVocabulary.value
        if (!currentList.any { it.word == item.word }) {
            persist(currentList + item.copy(frequency = 0))
        }
    }

    fun removeWord(item: VocabularyItem) {
        persist(_savedVocabulary.value.filter { it.word != item.word })
    }

    fun bumpFrequency(word: String) {
        val currentList = _savedVocabulary.value
        if (!currentList.any { it.word == word }) return
        persist(currentList.map { if (it.word == word) it.copy(frequency = it.frequency + 1) else it })
    }

    fun isSaved(word: String): Boolean {
        return _savedVocabulary.value.any { it.word == word }
    }
}
