package com.mettyoung.deconstructchinese.model

enum class RecordingPhase {
    Idle,       // not recording
    Armed,      // recognizer ready, waiting for sound ("Speak now")
    Listening   // sound detected, transcribing ("Listening…")
}
