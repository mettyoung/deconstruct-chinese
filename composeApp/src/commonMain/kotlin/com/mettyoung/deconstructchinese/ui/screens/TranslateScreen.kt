package com.mettyoung.deconstructchinese.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.model.RecordingPhase
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.ui.ImagePickerLauncher
import com.mettyoung.deconstructchinese.ui.components.ErrorCard
import com.mettyoung.deconstructchinese.ui.components.ImageSourceDialog
import com.mettyoung.deconstructchinese.ui.components.InputPanel
import com.mettyoung.deconstructchinese.ui.components.LanguageDirectionBar
import com.mettyoung.deconstructchinese.ui.components.TranslationResultCard
import com.mettyoung.deconstructchinese.ui.theme.*

@Composable
fun TranslateScreen(
    modifier: Modifier = Modifier,
    inputText: String,
    translationState: TranslationState,
    toEnglish: Boolean,
    isPlaying: Boolean,
    recordingPhase: RecordingPhase,
    isProcessingImage: Boolean,
    savedVocab: List<VocabularyItem>,
    apiKey: String,
    useSimplified: Boolean,
    imagePicker: ImagePickerLauncher,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onTranslate: () -> Unit,
    onSwapDirection: () -> Unit,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSaveWord: (VocabularyItem) -> Unit,
    onRemoveWord: (VocabularyItem) -> Unit,
    onOpenSettings: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    var showImageSourceDialog by remember { mutableStateOf(false) }

    if (showImageSourceDialog) {
        ImageSourceDialog(
            onCamera = { showImageSourceDialog = false; imagePicker.launchCamera() },
            onGallery = { showImageSourceDialog = false; imagePicker.launchGallery() },
            onDismiss = { showImageSourceDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        TranslateHeader(onOpenSettings = onOpenSettings)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            LanguageDirectionBar(
                toEnglish = toEnglish,
                useSimplified = useSimplified,
                onSwap = onSwapDirection,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            InputPanel(
                inputText = inputText,
                toEnglish = toEnglish,
                recordingPhase = recordingPhase,
                isProcessingImage = isProcessingImage,
                translationState = translationState,
                apiKey = apiKey,
                onInputChange = onInputChange,
                onClear = onClear,
                onTranslate = onTranslate,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onScanImage = { showImageSourceDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            AnimatedVisibility(
                visible = translationState is TranslationState.Success || translationState is TranslationState.Error,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    when (val state = translationState) {
                        is TranslationState.Success -> TranslationResultCard(
                            result = state.result,
                            toEnglish = toEnglish,
                            isPlaying = isPlaying,
                            savedVocab = savedVocab,
                            useSimplified = useSimplified,
                            onSpeak = onSpeak,
                            onStop = onStop,
                            onSpeakWord = onSpeakWord,
                            onSaveWord = onSaveWord,
                            onRemoveWord = onRemoveWord
                        )
                        is TranslationState.Error -> Box(Modifier.padding(horizontal = 16.dp)) {
                            ErrorCard(state.message)
                        }
                        else -> {}
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TranslateHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Deconstruct", 
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ), 
                color = TextPrimary
            )
            Text(
                "Chinese", 
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.5).sp
                ), 
                color = BluePrimary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Surface)
        ) {
            Icon(
                Icons.Default.Settings, 
                contentDescription = "Settings", 
                tint = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
