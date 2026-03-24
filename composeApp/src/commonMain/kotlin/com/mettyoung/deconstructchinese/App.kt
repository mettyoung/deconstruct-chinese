package com.mettyoung.deconstructchinese

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.viewmodel.TranslatorViewModel

// ── Colors ────────────────────────────────────────────────────────────────────
private val RedPrimary     = Color(0xFFD32F2F)
private val GoldAccent     = Color(0xFFF9A825)
private val BackgroundDark = Color(0xFF1A1A2E)
private val SurfaceDark    = Color(0xFF16213E)
private val CardDark       = Color(0xFF0F3460)
private val TextPrimary    = Color(0xFFF5F5F5)
private val TextSecondary  = Color(0xFFB0BEC5)
private val PinyinColor    = Color(0xFF80CBC4)

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary    = RedPrimary,
            secondary  = GoldAccent,
            background = BackgroundDark,
            surface    = SurfaceDark,
            onPrimary  = Color.White,
            onBackground = TextPrimary,
            onSurface  = TextPrimary,
        )
    ) {
        var apiKey by rememberSaveable { mutableStateOf("AIzaSyBdt5zkt1BiSNH7D8039EI4eGO3-urghsQ") }

        TranslatorScreen(
            apiKey = apiKey,
            onApiKeySubmit = { apiKey = it }
        )
    }
}

@Composable
fun ApiKeyModal(
    currentApiKey: String,
    onDismiss: () -> Unit,
    onApiKeySubmit: (String) -> Unit
) {
    var keyInput by remember { mutableStateOf(currentApiKey) }
    var showKey  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                "Gemini API Settings",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Enter your Gemini API key to enable translations.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API Key", color = TextSecondary) },
                    visualTransformation = if (showKey)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = RedPrimary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary
                    ),
                    singleLine = true
                )

                Text(
                    "Get a free key at aistudio.google.com",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { /* Link opening could be added here */ }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApiKeySubmit(keyInput.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun TranslatorScreen(apiKey: String, onApiKeySubmit: (String) -> Unit) {
    val viewModel: TranslatorViewModel = key(apiKey) {
        viewModel(
            factory = viewModelFactory {
                initializer {
                    TranslatorViewModel(apiKey)
                }
            }
        )
    }

    val inputText        by viewModel.inputText.collectAsStateWithLifecycle()
    val translationState by viewModel.translationState.collectAsStateWithLifecycle()
    val isPlaying        by viewModel.isPlaying.collectAsStateWithLifecycle()
    
    var showApiModal by remember { mutableStateOf(false) }

    if (showApiModal) {
        ApiKeyModal(
            currentApiKey = apiKey,
            onDismiss = { showApiModal = false },
            onApiKeySubmit = onApiKeySubmit
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Chinese Translator",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text("中文翻译器", color = GoldAccent, fontSize = 14.sp)
            }
            
            Row {
                if (translationState !is TranslationState.Idle) {
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.Refresh,
                            contentDescription = "Clear",
                            tint = TextSecondary)
                    }
                }
                IconButton(onClick = { showApiModal = true }) {
                    Icon(Icons.Default.Menu,
                        contentDescription = "Settings",
                        tint = TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Input card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("English", color = TextSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputTextChange(it) },
                    placeholder = { Text("Type an English sentence...",
                        color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = RedPrimary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = RedPrimary
                    ),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            showApiModal = true
                        } else {
                            viewModel.translate()
                        }
                    },
                    enabled = inputText.isNotBlank()
                            && translationState !is TranslationState.Loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    if (translationState is TranslationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Translating...")
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (apiKey.isBlank()) "Enter API Key to Translate" else "Translate to Chinese",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Results — only shown when state is Success or Error
        AnimatedVisibility(
            visible = translationState is TranslationState.Success
                    || translationState is TranslationState.Error,
            enter = fadeIn() + slideInVertically(),
            exit  = fadeOut()
        ) {
            when (val state = translationState) {
                is TranslationState.Success -> TranslationResultCard(
                    result    = state.result,
                    isPlaying = isPlaying,
                    onSpeak   = { viewModel.speakChinese() },
                    onStop    = { viewModel.stopAudio() },
                    onSpeakWord = { viewModel.speakWord(it) }
                )
                is TranslationState.Error -> ErrorCard(state.message)
                else -> {}
            }
        }
    }
}

@Composable
fun TranslationResultCard(
    result: TranslationResult,
    isPlaying: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onSpeakWord: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Main translation
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Chinese", color = GoldAccent,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    FilledTonalButton(
                        onClick = { if (isPlaying) onStop() else onSpeak() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isPlaying)
                                RedPrimary
                            else
                                GoldAccent.copy(alpha = 0.2f),
                            contentColor = if (isPlaying)
                                Color.White
                            else
                                GoldAccent
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop
                            else Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying) "Stop" else "Listen",
                            fontSize = 13.sp)
                    }
                }

                // Chinese characters (large)
                Text(
                    text = result.chineseText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 44.sp
                )

                // Pinyin
                Text(
                    text = result.pinyinText,
                    fontSize = 18.sp,
                    color = PinyinColor,
                    fontWeight = FontWeight.Medium
                )

                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

                // Original English
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("EN", color = TextSecondary, fontSize = 12.sp)
                    Text(result.originalText,
                        color = TextSecondary, fontSize = 15.sp)
                }
            }
        }

        // Grammar note
        if (result.grammarNote.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B3A4B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text("Grammar Note",
                            color = Color(0xFF4FC3F7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(result.grammarNote,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp)
                    }
                }
            }
        }

        // Vocabulary breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Vocabulary Breakdown",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp)

                Spacer(Modifier.height(4.dp))

                result.vocabulary.forEach { item ->
                    VocabularyCard(
                        item = item,
                        onSpeak = { onSpeakWord(item.character) }
                    )
                }
            }
        }
    }
}

@Composable
fun VocabularyCard(item: VocabularyItem, onSpeak: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Character box
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RedPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.character,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.width(12.dp))

        // Pinyin + meaning
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(item.pinyin,
                color = PinyinColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium)
            Text(item.meaning,
                color = TextSecondary,
                fontSize = 13.sp)
        }

        // Speak button
        IconButton(
            onClick = onSpeak,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GoldAccent.copy(alpha = 0.15f))
        ) {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = "Pronounce ${item.character}",
                tint = GoldAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3E1B1B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF6659))
            Column {
                Text("Translation Error",
                    color = Color(0xFFFF6659),
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(message,
                    color = TextSecondary,
                    fontSize = 14.sp)
            }
        }
    }
}
