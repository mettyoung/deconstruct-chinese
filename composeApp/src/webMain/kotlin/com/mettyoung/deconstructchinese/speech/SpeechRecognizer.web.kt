package com.mettyoung.deconstructchinese.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class SpeechRecognizer actual constructor() {
    actual val results: Flow<SpeechResult> = flowOf(SpeechResult.Error("unsupported"))
    actual fun startListening(locale: String) {}
    actual fun stopListening() {}
    actual fun release() {}
}
