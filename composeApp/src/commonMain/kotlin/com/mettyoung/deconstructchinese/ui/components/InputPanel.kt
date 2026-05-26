package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.isWebPlatform
import com.mettyoung.deconstructchinese.model.RecordingPhase
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary
import com.mettyoung.deconstructchinese.ui.theme.Divider
import com.mettyoung.deconstructchinese.ui.theme.Surface
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@Composable
fun InputPanel(
    inputText: String,
    toEnglish: Boolean,
    recordingPhase: RecordingPhase,
    isProcessingImage: Boolean,
    translationState: TranslationState,
    apiKey: String,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onTranslate: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onScanImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val recording = recordingPhase != RecordingPhase.Idle

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box {
                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        val hint = when (recordingPhase) {
                            RecordingPhase.Armed -> "Speak now"
                            RecordingPhase.Listening -> "Listening…"
                            RecordingPhase.Idle ->
                                if (toEnglish) "Type or paste Chinese..." else "Type English text..."
                        }
                        Crossfade(targetState = hint, label = "placeholder") { text ->
                            Text(
                                text,
                                color = if (recording) BluePrimary else TextSecondary.copy(alpha = 0.4f),
                                fontSize = 20.sp
                            )
                        }
                    },
                    textStyle = TextStyle(fontSize = 20.sp, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BluePrimary
                    )
                )
                if (inputText.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (inputText.isEmpty()) {
                    TextButton(
                        onClick = {
                            val pasted = clipboardManager.getText()?.text.orEmpty()
                            if (pasted.isNotEmpty()) onInputChange(pasted)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Paste", fontSize = 13.sp)
                    }

                    Spacer(Modifier.weight(1f))

                    if (!isWebPlatform) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isProcessingImage) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BluePrimary, strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = onScanImage, enabled = !recording) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan image", tint = TextSecondary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            MicButton(
                                recordingPhase = recordingPhase,
                                onStartRecording = onStartRecording,
                                onStopRecording = onStopRecording
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    if (translationState is TranslationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 8.dp),
                            color = BluePrimary,
                            strokeWidth = 2.dp
                        )
                    } else if (translationState !is TranslationState.Success) {
                        Button(
                            onClick = onTranslate,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(if (apiKey.isBlank()) "Setup API" else "Translate", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
