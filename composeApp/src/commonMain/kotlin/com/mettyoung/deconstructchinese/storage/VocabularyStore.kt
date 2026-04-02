package com.mettyoung.deconstructchinese.storage

import com.mettyoung.deconstructchinese.model.VocabularyItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VocabularyStore {
    private val _savedVocabulary = MutableStateFlow<List<VocabularyItem>>(emptyList())
    val savedVocabulary: StateFlow<List<VocabularyItem>> = _savedVocabulary.asStateFlow()

    fun saveWord(item: VocabularyItem) {
        if (!_savedVocabulary.value.any { it.character == item.character }) {
            _savedVocabulary.value += item
        }
    }

    fun removeWord(item: VocabularyItem) {
        _savedVocabulary.value = _savedVocabulary.value.filter { it.character != item.character }
    }
    
    fun isSaved(character: String): Boolean {
        return _savedVocabulary.value.any { it.character == character }
    }
}
