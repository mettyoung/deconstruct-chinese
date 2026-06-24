package com.mettyoung.deconstructchinese.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

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

    actual fun playListenCue() {
        // Synthesized rising two-note chime (G5 -> C6), à la a voice-assistant
        // "ready to listen" blip. No bundled asset, generated on the fly.
        Thread {
            runCatching {
                val sampleRate = 44_100
                val notes = doubleArrayOf(783.99, 1046.50) // G5, C6
                val noteSamples = sampleRate * 95 / 1000    // ~95ms per note
                val fade = 240                              // click-free in/out ramp
                val buf = ShortArray(noteSamples * notes.size)
                var i = 0
                for (freq in notes) {
                    for (n in 0 until noteSamples) {
                        val env = when {
                            n < fade -> n.toDouble() / fade
                            n > noteSamples - fade -> (noteSamples - n).toDouble() / fade
                            else -> 1.0
                        }
                        val amp = Short.MAX_VALUE * 0.32 * env
                        buf[i++] = (amp * sin(2.0 * PI * freq * n / sampleRate)).toInt().toShort()
                    }
                }
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buf.size * 2,
                    AudioTrack.MODE_STATIC
                )
                track.write(buf, 0, buf.size)
                track.play()
                // Let it finish, then free the track.
                Thread.sleep((notes.size * 95 + 60).toLong())
                track.stop()
                track.release()
            }
        }.start()
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