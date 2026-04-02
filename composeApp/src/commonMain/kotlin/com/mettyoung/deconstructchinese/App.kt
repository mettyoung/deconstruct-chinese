package com.mettyoung.deconstructchinese

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.ui.components.TranslationResultCard
import com.mettyoung.deconstructchinese.ui.screens.VocabularyScreen
import com.mettyoung.deconstructchinese.ui.theme.*
import com.mettyoung.deconstructchinese.viewmodel.TranslatorViewModel

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary    = RedPrimary,
            secondary  = GoldAccent,
            background = BackgroundDark,
            surface    = SurfaceDark,
            onPrimary  = androidx.compose.ui.graphics.Color.White,
            onBackground = TextPrimary,
            onSurface  = TextPrimary,
        )
    ) {
        var apiKey by rememberSaveable { mutableStateOf("sk-043c8d868fed44758bb76d84774aeeea") }

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
                "Qwen API Settings",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Enter your Alibaba DashScope API key to enable translations.",
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
                    "Get your key at dashscope.console.aliyun.com",
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
    val savedVocab       by viewModel.savedVocabulary.collectAsStateWithLifecycle()
    
    var showApiModal by remember { mutableStateOf(false) }
    var showCollection by remember { mutableStateOf(false) }

    if (showApiModal) {
        ApiKeyModal(
            currentApiKey = apiKey,
            onDismiss = { showApiModal = false },
            onApiKeySubmit = onApiKeySubmit
        )
    }

    if (showCollection) {
        VocabularyScreen(
            vocabulary = savedVocab,
            onDismiss = { showCollection = false },
            onRemove = { viewModel.removeWord(it) },
            onSpeak = { viewModel.speakWord(it) }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .safeDrawingPadding()
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
                    Text("Powered by Qwen", color = GoldAccent, fontSize = 14.sp)
                }
                
                Row {
                    if (translationState !is TranslationState.Idle) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(Icons.Default.Refresh,
                                contentDescription = "Clear",
                                tint = TextSecondary)
                        }
                    }
                    IconButton(onClick = { showCollection = true }) {
                        Icon(Icons.Default.CollectionsBookmark,
                            contentDescription = "Saved Vocabulary",
                            tint = if (savedVocab.isNotEmpty()) GoldAccent else TextPrimary)
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
                                color = androidx.compose.ui.graphics.Color.White,
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
                        savedVocab = savedVocab,
                        onSpeak   = { viewModel.speakChinese() },
                        onStop    = { viewModel.stopAudio() },
                        onSpeakWord = { viewModel.speakWord(it) },
                        onSaveWord = { viewModel.saveWord(it) },
                        onRemoveWord = { viewModel.removeWord(it) }
                    )
                    is TranslationState.Error -> com.mettyoung.deconstructchinese.ui.components.ErrorCard(state.message)
                    else -> {}
                }
            }
        }
    }
}
