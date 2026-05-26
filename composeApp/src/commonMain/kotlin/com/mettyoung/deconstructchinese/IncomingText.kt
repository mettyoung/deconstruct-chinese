package com.mettyoung.deconstructchinese

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Bus for text handed to the app from outside (Android PROCESS_TEXT/SEND intents,
 * iOS share extension via URL scheme). CONFLATED so a value submitted before the UI
 * starts collecting (cold start) is buffered and delivered once.
 */
object IncomingText {
    private val channel = Channel<String>(Channel.CONFLATED)
    val texts: Flow<String> = channel.receiveAsFlow()

    fun submit(text: String) {
        if (text.isNotBlank()) channel.trySend(text)
    }
}

/** Convenience top-level entry for Swift: `IncomingTextKt.submitSharedText("…")`. */
fun submitSharedText(text: String) = IncomingText.submit(text)
