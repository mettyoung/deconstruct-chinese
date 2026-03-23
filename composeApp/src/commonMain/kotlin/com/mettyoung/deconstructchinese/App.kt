package com.mettyoung.deconstructchinese

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
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
        var apiKey by remember { mutableStateOf("") }
        var apiKeyEntered by remember { mutableStateOf(false) }

        if (!apiKeyEntered) {
            ApiKeyScreen(
                onApiKeySubmit = { key ->
                    apiKey = key
                    apiKeyEntered = true
                }
            )
        } else {
            TranslatorScreen(apiKey = apiKey)
        }
    }
}

@Composable
fun ApiKeyScreen(onApiKeySubmit: (String) -> Unit) {
    var keyInput by remember { mutableStateOf("") }
    var showKey  by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Text("🀄", fontSize = 64.sp)
            Text(
                "Chinese Translator",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Powered by Gemini AI",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("How to get your free API key:",
                        color = GoldAccent, fontWeight = FontWeight.SemiBold)
                    Text("1. Go to aistudio.google.com",
                        color = TextSecondary, fontSize = 13.sp)
                    Text("2. Sign in with your Google account",
                        color = TextSecondary, fontSize = 13.sp)
                    Text("3. Click Get API Key → Create API Key",
                        color = TextSecondary, fontSize = 13.sp)
                    Text("4. Copy and paste it below",
                        color = TextSecondary, fontSize = 13.sp)
                    Text("Free tier: 15 requests/min, 1500/day",
                        color = Color(0xFF81C784), fontSize = 13.sp)
                }
            }

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Gemini API Key", color = TextSecondary) },
                placeholder = { Text("AIza...",
                    color = TextSecondary.copy(alpha = 0.5f)) },
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

            Button(
                onClick = {
                    if (keyInput.isNotBlank())
                        onApiKeySubmit(keyInput.trim())
                },
                enabled = keyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                Text("Start Learning Chinese!", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TranslatorScreen(apiKey: String) {
    val viewModel: TranslatorViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TranslatorViewModel(apiKey)
            }
        }
    )

    val inputText        by viewModel.inputText.collectAsStateWithLifecycle()
    val translationState by viewModel.translationState.collectAsStateWithLifecycle()
    val isPlaying        by viewModel.isPlaying.collectAsStateWithLifecycle()

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
            if (translationState !is TranslationState.Idle) {
                IconButton(onClick = { viewModel.clearAll() }) {
                    Icon(Icons.Default.Refresh,
                        contentDescription = "Clear",
                        tint = TextSecondary)
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
                    onClick = { viewModel.translate() },
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
                        Text("Translate to Chinese",
                            fontWeight = FontWeight.SemiBold)
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
