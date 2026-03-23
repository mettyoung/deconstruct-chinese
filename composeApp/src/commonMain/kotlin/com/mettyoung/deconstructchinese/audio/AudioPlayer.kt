package com.mettyoung.deconstructchinese.audio

expect class AudioPlayer() {
    fun speak(text: String, language: String = "zh-CN")
    fun stop()
    fun release()
}