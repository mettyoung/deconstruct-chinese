package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mettyoung.deconstructchinese.ui.theme.GoldAccent
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@Composable
fun MicButton(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mic")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "micAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(alpha = pulseAlpha * 0.45f))
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isRecording) GoldAccent else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        onStartRecording()
                        try {
                            awaitRelease()
                        } finally {
                            onStopRecording()
                        }
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Hold to record",
                tint = if (isRecording) Color.White else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
