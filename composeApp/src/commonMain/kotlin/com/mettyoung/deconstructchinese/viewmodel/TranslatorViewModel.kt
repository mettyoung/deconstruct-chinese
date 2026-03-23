package com.mettyoung.deconstructchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mettyoung.deconstructchinese.audio.AudioPlayer
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.network.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslatorViewModel(apiKey: String) : ViewModel() {

    private val geminiService = GeminiService(apiKey)
    private val audioPlayer = AudioPlayer()

    private val _translationState =
        MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> =
        _translationState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
        if (_translationState.value is TranslationState.Success) {
            _translationState.value = TranslationState.Idle
        }
    }

    fun translate() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _translationState.value = TranslationState.Loading

            try {
                val result = geminiService.translate(text)
                _translationState.value = TranslationState.Success(result)
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(
                    message = when {
                        e.message?.contains("400") == true ->
                            "Invalid API key. Please check your Gemini API key."
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

    fun speakChinese() {
        val state = _translationState.value
        if (state is TranslationState.Success) {
            _isPlaying.value = true
            audioPlayer.speak(state.result.chineseText, "zh-CN")
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                _isPlaying.value = false
            }
        }
    }

    fun speakWord(word: String) {
        audioPlayer.speak(word, "zh-CN")
    }

    fun stopAudio() {
        audioPlayer.stop()
        _isPlaying.value = false
    }

    fun clearAll() {
        stopAudio()
        _inputText.value = ""
        _translationState.value = TranslationState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}