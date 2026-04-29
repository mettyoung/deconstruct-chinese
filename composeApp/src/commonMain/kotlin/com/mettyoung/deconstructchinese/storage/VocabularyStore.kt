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
        return settings.decodeValueOrNull<List<VocabularyItem>>(KEY_VOCABULARY) ?: emptyList()
    }

    private fun saveToSettings(list: List<VocabularyItem>) {
        settings.encodeValue(KEY_VOCABULARY, list)
    }

    fun saveWord(item: VocabularyItem) {
        val currentList = _savedVocabulary.value
        if (!currentList.any { it.word == item.word }) {
            val newList = currentList + item
            _savedVocabulary.value = newList
            saveToSettings(newList)
        }
    }

    fun removeWord(item: VocabularyItem) {
        val newList = _savedVocabulary.value.filter { it.word != item.word }
        _savedVocabulary.value = newList
        saveToSettings(newList)
    }
    
    fun isSaved(word: String): Boolean {
        return _savedVocabulary.value.any { it.word == word }
    }
}
