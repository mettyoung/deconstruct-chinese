package com.mettyoung.deconstructchinese.speech

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import com.mettyoung.deconstructchinese.audio.AppContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

actual class SpeechRecognizer actual constructor() {
    private val _results = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 10)
    actual val results: Flow<SpeechResult> = _results.asSharedFlow()

    private var recognizer: AndroidSpeechRecognizer? = null

    actual fun startListening(locale: String) {
        val ctx = AppContext.get()
        recognizer?.destroy()
        recognizer = AndroidSpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle) {
                    _results.tryEmit(SpeechResult.Ready)
                }
                override fun onBeginningOfSpeech() {
                    _results.tryEmit(SpeechResult.SpeechStarted)
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle) {}

                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                    _results.tryEmit(SpeechResult.Final(matches?.firstOrNull() ?: ""))
                }

                override fun onPartialResults(partialResults: Bundle) {
                    val matches = partialResults.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                    _results.tryEmit(SpeechResult.Partial(matches?.firstOrNull() ?: ""))
                }

                override fun onError(error: Int) {
                    when (error) {
                        // Silent cancels: a momentary tap or rapid re-tap tears the
                        // recognizer down before it captures speech. ERROR_SERVER_DISCONNECTED
                        // (11) and ERROR_RECOGNIZER_BUSY (8) are that case — not real failures.
                        AndroidSpeechRecognizer.ERROR_CLIENT,
                        AndroidSpeechRecognizer.ERROR_NO_MATCH,
                        AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        AndroidSpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        AndroidSpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                            _results.tryEmit(SpeechResult.Cancelled)
                        AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            _results.tryEmit(SpeechResult.Error("Microphone permission denied"))
                        else ->
                            _results.tryEmit(SpeechResult.Error("Recognition error ($error)"))
                    }
                }
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.startListening(intent)
    }

    actual fun stopListening() {
        recognizer?.stopListening()
    }

    actual fun release() {
        recognizer?.destroy()
        recognizer = null
    }
}
