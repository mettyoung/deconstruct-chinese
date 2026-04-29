package com.mettyoung.deconstructchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mettyoung.deconstructchinese.audio.AudioPlayer
import com.mettyoung.deconstructchinese.model.Language
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.network.QwenService
import com.mettyoung.deconstructchinese.storage.VocabularyStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslatorViewModel(apiKey: String) : ViewModel() {

    private val qwenService = QwenService(apiKey)
    private val audioPlayer = AudioPlayer()

    private val _translationState =
        MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> =
        _translationState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val savedVocabulary = VocabularyStore.savedVocabulary

    private var translateJob: Job? = null

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
        if (_translationState.value is TranslationState.Success) {
            _translationState.value = TranslationState.Idle
        }
        translateJob?.cancel()
        if (newText.isNotBlank()) {
            translateJob = viewModelScope.launch {
                delay(800L)
                translate()
            }
        }
    }

    fun translate() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _translationState.value is TranslationState.Loading) return

        viewModelScope.launch {
            _translationState.value = TranslationState.Loading

            try {
                val result = qwenService.translate(text, Language.ENGLISH, Language.CHINESE_TRADITIONAL)
                result.vocabulary.forEach { item ->
                    if (VocabularyStore.isSaved(item.word)) {
                        VocabularyStore.bumpFrequency(item.word)
                    }
                }
                _translationState.value = TranslationState.Success(result)
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(
                    message = when {
                        e.message?.contains("401") == true ->
                            "Invalid API key. Please check your Qwen API key."
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

    fun speakTranslation() {
        val state = _translationState.value
        if (state is TranslationState.Success) {
            _isPlaying.value = true
            audioPlayer.speak(state.result.translatedText, state.result.targetLanguage.code)
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                _isPlaying.value = false
            }
        }
    }

    fun speakWord(word: String) {
        val state = _translationState.value
        if (state is TranslationState.Success) {
            audioPlayer.speak(word, state.result.targetLanguage.code)
        } else {
            // Fallback for saved vocabulary where we don't have the context of the current target language
            // For now, default to Chinese Traditional as it was the original focus
            audioPlayer.speak(word, Language.CHINESE_TRADITIONAL.code)
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
        _isPlaying.value = false
    }

    fun clearAll() {
        translateJob?.cancel()
        stopAudio()
        _inputText.value = ""
        _translationState.value = TranslationState.Idle
    }

    fun saveWord(item: VocabularyItem) {
        VocabularyStore.saveWord(item)
    }

    fun removeWord(item: VocabularyItem) {
        VocabularyStore.removeWord(item)
    }

    fun isSaved(word: String): Boolean {
        return VocabularyStore.isSaved(word)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
