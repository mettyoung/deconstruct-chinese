package com.mettyoung.deconstructchinese.speech

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AVFAudio.*
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Speech.*

@OptIn(ExperimentalForeignApi::class)
actual class SpeechRecognizer actual constructor() {
    private val _results = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 10)
    actual val results: Flow<SpeechResult> = _results.asSharedFlow()

    private val audioEngine = AVAudioEngine()
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var sfRecognizer: SFSpeechRecognizer? = null

    actual fun startListening(locale: String) {
        SFSpeechRecognizer.requestAuthorization { status ->
            if (status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    if (granted) {
                        startRecognition(locale)
                    } else {
                        _results.tryEmit(SpeechResult.Error("Microphone permission denied"))
                    }
                }
            } else {
                _results.tryEmit(SpeechResult.Error("Speech recognition permission denied"))
            }
        }
    }

    private fun startRecognition(locale: String) {
        sfRecognizer = SFSpeechRecognizer(NSLocale(locale))
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest().also {
            it.shouldReportPartialResults = true
        }

        recognitionTask = sfRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            error?.let { _results.tryEmit(SpeechResult.Error(it.localizedDescription)) }
            result?.let {
                val text = it.bestTranscription.formattedString
                if (it.isFinal) {
                    _results.tryEmit(SpeechResult.Final(text))
                } else {
                    _results.tryEmit(SpeechResult.Partial(text))
                }
            }
        }

        val inputNode = audioEngine.inputNode
        val format = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            buffer?.let { recognitionRequest?.appendAudioPCMBuffer(it) }
        }

        audioEngine.prepare()
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val started = audioEngine.startAndReturnError(errorPtr.ptr)
            if (!started) {
                val msg = errorPtr.value?.localizedDescription ?: "Audio engine failed to start"
                _results.tryEmit(SpeechResult.Error(msg))
            }
        }
    }

    actual fun stopListening() {
        if (audioEngine.isRunning) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }
        recognitionRequest?.endAudio()
        recognitionRequest = null
    }

    actual fun release() {
        stopListening()
        recognitionTask?.cancel()
        recognitionTask = null
        sfRecognizer = null
    }
}
