package com.mettyoung.deconstructchinese.audio

actual class AudioPlayer actual constructor() {
    actual fun speak(text: String, language: String) {}
    actual fun stop() {}
    actual fun playListenCue() {}
    actual fun release() {}
}
