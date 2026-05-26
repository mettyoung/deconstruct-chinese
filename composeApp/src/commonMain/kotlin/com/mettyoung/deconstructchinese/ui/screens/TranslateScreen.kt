package com.mettyoung.deconstructchinese.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mettyoung.deconstructchinese.model.RecordingPhase
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.ui.ImagePickerLauncher
import com.mettyoung.deconstructchinese.ui.components.ErrorCard
import com.mettyoung.deconstructchinese.ui.components.ImageSourceDialog
import com.mettyoung.deconstructchinese.ui.components.InputPanel
import com.mettyoung.deconstructchinese.ui.components.LanguageDirectionBar
import com.mettyoung.deconstructchinese.ui.components.TranslationResultCard
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary
import com.mettyoung.deconstructchinese.ui.theme.Card
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

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

        LanguageDirectionBar(
            toEnglish = toEnglish,
            useSimplified = useSimplified,
            onSwap = onSwapDirection,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
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

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TranslateHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Deconstruct", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text("Chinese", style = MaterialTheme.typography.headlineSmall.copy(color = BluePrimary))
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.clip(CircleShape).background(Card)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
        }
    }
}
