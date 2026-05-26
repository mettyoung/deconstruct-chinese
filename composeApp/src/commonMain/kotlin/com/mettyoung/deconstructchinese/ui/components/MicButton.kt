package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary

private val ButtonSize = 64.dp
private val ContainerSize = 96.dp

@Composable
fun MicButton(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mic")
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "micRipple"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ContainerSize)) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(ButtonSize)
                    .graphicsLayer {
                        val scale = 1f + ripple * 0.5f
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - ripple) * 0.45f
                    }
                    .clip(CircleShape)
                    .background(BluePrimary)
            )
        }
        Box(
            modifier = Modifier
                .size(ButtonSize)
                .shadow(if (isRecording) 10.dp else 6.dp, CircleShape)
                .clip(CircleShape)
                .background(BluePrimary)
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
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
