package com.mettyoung.deconstructchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mettyoung.deconstructchinese.audio.AudioPlayer
import com.mettyoung.deconstructchinese.model.Language
import com.mettyoung.deconstructchinese.model.RecordingPhase
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.network.TranslationService
import com.mettyoung.deconstructchinese.ocr.OcrLanguage
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TranslatorViewModel(
    private val translationService: TranslationService
) : ViewModel() {

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

    private val _recordingPhase = MutableStateFlow(RecordingPhase.Idle)
    val recordingPhase: StateFlow<RecordingPhase> = _recordingPhase.asStateFlow()

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
                    is SpeechResult.Ready -> {
                        if (_recordingPhase.value != RecordingPhase.Idle) {
                            _recordingPhase.value = RecordingPhase.Armed
                        }
                    }
                    is SpeechResult.SpeechStarted -> {
                        if (_recordingPhase.value != RecordingPhase.Idle) {
                            _recordingPhase.value = RecordingPhase.Listening
                        }
                    }
                    is SpeechResult.Partial -> {
                        // Stream recognized words live without scheduling a translate.
                        if (_recordingPhase.value != RecordingPhase.Idle) {
                            _inputText.value = result.text
                        }
                    }
                    is SpeechResult.Final -> {
                        _recordingPhase.value = RecordingPhase.Idle
                        onInputTextChange(result.text)
                    }
                    is SpeechResult.Cancelled -> {
                        _recordingPhase.value = RecordingPhase.Idle
                    }
                    is SpeechResult.Error -> {
                        _recordingPhase.value = RecordingPhase.Idle
                        _snackbarMessage.tryEmit(result.message)
                    }
                }
            }
        }
    }

    fun onSharedText(text: String) {
        // Set the source side to match the shared text's script so it translates correctly.
        val hasHan = text.any { it.code in 0x4E00..0x9FFF }
        _toEnglish.value = hasHan
        onInputTextChange(text)
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
        if (text.isEmpty()) return

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            _translationState.value = TranslationState.Loading
            val toEng = _toEnglish.value
            val simp = _useSimplified.value
            val chineseLang =
                if (simp) Language.CHINESE_SIMPLIFIED else Language.CHINESE_TRADITIONAL

            try {
                // Stage 1: stream translation + sentence pinyin for fast first paint.
                translationService.translateStream(text, toEng, simp).collect { partial ->
                    _translationState.value = TranslationState.Success(
                        result = partialResult(text, partial.translation, partial.pinyin, toEng, chineseLang),
                        vocabLoading = true
                    )
                }

                // Stage 2: full breakdown (vocabulary, pinyin) replaces the partial.
                val full = translationService.translate(text, toEng, simp)
                full.vocabulary.forEach { item ->
                    if (VocabularyStore.isSaved(item.word)) {
                        VocabularyStore.bumpFrequency(item)
                    }
                }
                _translationState.value = TranslationState.Success(full, vocabLoading = false)
            } catch (e: Exception) {
                // Keep a streamed translation if we already have one — only the
                // breakdown failed. Otherwise surface the error.
                val current = _translationState.value
                if (current is TranslationState.Success) {
                    _translationState.value = current.copy(vocabLoading = false)
                    _snackbarMessage.tryEmit("Vocabulary breakdown unavailable.")
                } else {
                    _translationState.value = TranslationState.Error(mapError(e))
                }
            }
        }
    }

    private fun partialResult(
        original: String,
        translated: String,
        phonetic: String,
        toEnglish: Boolean,
        chineseLang: Language
    ) = TranslationResult(
        originalText = original,
        translatedText = translated,
        chineseText = if (toEnglish) original else translated,
        phoneticText = phonetic,
        vocabulary = emptyList(),
        grammarNote = "",
        sourceLanguage = if (toEnglish) chineseLang else Language.ENGLISH,
        targetLanguage = if (toEnglish) Language.ENGLISH else chineseLang
    )

    private fun mapError(e: Exception): String = when {
        e.message?.contains("401") == true ->
            "Invalid API key. Please check your API key."
        e.message?.contains("429") == true ->
            "Rate limit reached. Wait a moment and try again."
        e.message?.contains("connect") == true ->
            "Network error. Check your internet connection."
        else -> "Translation failed: ${e.message}"
    }

    fun startRecording() {
        translateJob?.cancel()
        _inputText.value = ""
        _translationState.value = TranslationState.Idle
        _recordingPhase.value = RecordingPhase.Armed
        // Source side of the toggle: toEnglish == translating Chinese -> English.
        val locale = if (_toEnglish.value) {
            if (_useSimplified.value) "zh-CN" else "zh-TW"
        } else {
            "en-US"
        }
        speechRecognizer.startListening(locale)
    }

    fun stopRecording() {
        if (_recordingPhase.value != RecordingPhase.Idle) {
            _recordingPhase.value = RecordingPhase.Idle
        }
        speechRecognizer.stopListening()
    }

    fun processImage(imageBytes: ByteArray) {
        // Source side of the toggle: toEnglish == translating Chinese -> English.
        val language = if (_toEnglish.value) OcrLanguage.CHINESE else OcrLanguage.ENGLISH
        _isProcessingImage.value = true
        viewModelScope.launch {
            ocrReader.recognizeText(imageBytes, language).collect { result ->
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
