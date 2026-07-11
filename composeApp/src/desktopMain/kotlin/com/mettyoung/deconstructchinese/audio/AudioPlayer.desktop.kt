package com.mettyoung.deconstructchinese.audio

import java.awt.Toolkit

actual class AudioPlayer actual constructor() {
    // No cross-platform desktop TTS bundled; speech playback is a no-op for now.
    actual fun speak(text: String, language: String) {}
    actual fun stop() {}
    actual fun playListenCue() {
        runCatching { Toolkit.getDefaultToolkit().beep() }
    }
    actual fun release() {}
}
