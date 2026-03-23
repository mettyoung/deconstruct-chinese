package com.mettyoung.deconstructchinese.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

actual class AudioPlayer actual constructor() {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        val ctx = AppContext.get()
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
            }
        }
    }

    actual fun speak(text: String, language: String) {
        if (!isReady) return
        val locale = when (language) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.SIMPLIFIED_CHINESE
        }
        tts?.setLanguage(locale)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }

    actual fun stop() {
        tts?.stop()
    }

    actual fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

// Singleton that holds Android's Context
// needed because AudioPlayer is created from shared code
// which has no access to Android APIs directly
object AppContext {
    private var context: Context? = null

    fun set(ctx: Context) {
        context = ctx.applicationContext
    }

    fun get(): Context = context ?: throw IllegalStateException(
        "AppContext not initialized! Call AppContext.set(context) in MainActivity."
    )
}