package com.mettyoung.deconstructchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mettyoung.deconstructchinese.audio.AudioPlayer
import com.mettyoung.deconstructchinese.model.Language
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.network.QwenService
import com.mettyoung.deconstructchinese.ocr.OcrReader
import com.mettyoung.deconstructchinese.ocr.OcrResult
import com.mettyoung.deconstructchinese.speech.SpeechRecognizer
import com.mettyoung.deconstructchinese.speech.SpeechResult
import com.mettyoung.deconstructchinese.storage.AppSettings
import com.mettyoung.deconstructchinese.storage.VocabularyStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslatorViewModel(apiKey: String) : ViewModel() {

    private val qwenService = QwenService(apiKey)
    private val audioPlayer = AudioPlayer()
    private val speechRecognizer = SpeechRecognizer()
    private val ocrReader = OcrReader()

    private val _translationState =
        MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> =
        _translationState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isProcessingImage = MutableStateFlow(false)
    val isProcessingImage: StateFlow<Boolean> = _isProcessingImage.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val savedVocabulary = VocabularyStore.savedVocabulary

    private val _toEnglish = MutableStateFlow(false)
    val toEnglish: StateFlow<Boolean> = _toEnglish.asStateFlow()

    private val _useSimplified = MutableStateFlow(AppSettings.useSimplified)
    val useSimplified: StateFlow<Boolean> = _useSimplified.asStateFlow()

    private var translateJob: Job? = null

    init {
        viewModelScope.launch {
            speechRecognizer.results.collect { result ->
                when (result) {
                    is SpeechResult.Final -> {
                        _isRecording.value = false
                        onInputTextChange(result.text)
                    }
                    is SpeechResult.Error -> {
                        _isRecording.value = false
                        _snackbarMessage.tryEmit(result.message)
                    }
                    is SpeechResult.Partial -> {}
                }
            }
        }
    }

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

    fun swapDirection() {
        translateJob?.cancel()
        _toEnglish.value = !_toEnglish.value
        _inputText.value = ""
        _translationState.value = TranslationState.Idle
        stopAudio()
    }

    fun translate() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _translationState.value is TranslationState.Loading) return

        viewModelScope.launch {
            _translationState.value = TranslationState.Loading

            try {
                val result = qwenService.translate(text, _toEnglish.value, _useSimplified.value)
                result.vocabulary.forEach { item ->
                    if (VocabularyStore.isSaved(item.word)) {
                        VocabularyStore.bumpFrequency(item)
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

    fun startRecording() {
        translateJob?.cancel()
        _inputText.value = ""
        _translationState.value = TranslationState.Idle
        _isRecording.value = true
        val locale = if (_toEnglish.value) "en-US" else "zh-CN"
        speechRecognizer.startListening(locale)
    }

    fun stopRecording() {
        _isRecording.value = false
        speechRecognizer.stopListening()
    }

    fun processImage(imageBytes: ByteArray) {
        _isProcessingImage.value = true
        viewModelScope.launch {
            ocrReader.recognizeText(imageBytes).collect { result ->
                _isProcessingImage.value = false
                when (result) {
                    is OcrResult.Success -> onInputTextChange(result.text)
                    is OcrResult.Error -> _snackbarMessage.tryEmit(result.message)
                }
            }
        }
    }

    fun speakTranslation() {
        val state = _translationState.value
        if (state is TranslationState.Success) {
            _isPlaying.value = true
            audioPlayer.speak(state.result.chineseText, Language.CHINESE_TRADITIONAL.code)
            viewModelScope.launch {
                delay(3000)
                _isPlaying.value = false
            }
        }
    }

    fun speakWord(word: String) {
        audioPlayer.speak(word, Language.CHINESE_TRADITIONAL.code)
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

    fun setUseSimplified(value: Boolean) {
        AppSettings.useSimplified = value
        _useSimplified.value = value
        _translationState.value = TranslationState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        speechRecognizer.release()
    }
}
