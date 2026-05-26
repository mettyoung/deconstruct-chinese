package com.mettyoung.deconstructchinese.speech

sealed class SpeechResult {
    data object Ready : SpeechResult()           // recognizer armed, awaiting sound
    data object SpeechStarted : SpeechResult()   // sound/speech detected
    data object Cancelled : SpeechResult()       // benign cancel (client/no-match/timeout)
    data class Partial(val text: String) : SpeechResult()
    data class Final(val text: String) : SpeechResult()
    data class Error(val message: String) : SpeechResult()
}
