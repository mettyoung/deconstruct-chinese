package com.mettyoung.deconstructchinese.audio

expect class AudioPlayer() {
    fun speak(text: String, language: String = "zh-CN")
    fun stop()
    /** Short cue played when the recognizer is armed ("Speak now"). */
    fun playListenCue()
    fun release()
}