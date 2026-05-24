package com.mettyoung.deconstructchinese.speech

import kotlinx.coroutines.flow.Flow

expect class SpeechRecognizer() {
    val results: Flow<SpeechResult>
    fun startListening(locale: String)
    fun stopListening()
    fun release()
}
