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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mettyoung.deconstructchinese.model.TranslationState
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.ui.components.ErrorCard
import com.mettyoung.deconstructchinese.ui.components.TranslationResultCard
import com.mettyoung.deconstructchinese.ui.screens.VocabularyScreen
import com.mettyoung.deconstructchinese.ui.theme.Background
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary
import com.mettyoung.deconstructchinese.ui.theme.Divider
import com.mettyoung.deconstructchinese.ui.theme.GoldAccent
import com.mettyoung.deconstructchinese.ui.theme.Surface
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary
import com.mettyoung.deconstructchinese.viewmodel.TranslatorViewModel

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary      = BluePrimary,
            secondary    = GoldAccent,
            background   = Background,
            surface      = Surface,
            onPrimary    = Color.White,
            onBackground = TextPrimary,
            onSurface    = TextPrimary,
        )
    ) {
        var apiKey by rememberSaveable { mutableStateOf("sk-043c8d868fed44758bb76d84774aeeea") }
        TranslatorScreen(apiKey = apiKey, onApiKeySubmit = { apiKey = it })
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
        containerColor = Surface,
        title = {
            Text("API Settings", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Enter your API key to enable translations.",
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
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BluePrimary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeySubmit(keyInput.trim()); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun TranslatorScreen(apiKey: String, onApiKeySubmit: (String) -> Unit) {
    val viewModel: TranslatorViewModel = key(apiKey) {
        viewModel(
            factory = viewModelFactory { initializer { TranslatorViewModel(apiKey) } }
        )
    }

    val inputText        by viewModel.inputText.collectAsStateWithLifecycle()
    val translationState by viewModel.translationState.collectAsStateWithLifecycle()
    val isPlaying        by viewModel.isPlaying.collectAsStateWithLifecycle()
    val savedVocab       by viewModel.savedVocabulary.collectAsStateWithLifecycle()
    val toEnglish        by viewModel.toEnglish.collectAsStateWithLifecycle()

    var showApiModal by remember { mutableStateOf(false) }
    var selectedTab  by rememberSaveable { mutableIntStateOf(0) }

    if (showApiModal) {
        ApiKeyModal(
            currentApiKey = apiKey,
            onDismiss = { showApiModal = false },
            onApiKeySubmit = onApiKeySubmit
        )
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                    label = { Text("Translate") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = BluePrimary,
                        selectedTextColor   = BluePrimary,
                        indicatorColor      = BluePrimary.copy(alpha = 0.12f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (savedVocab.isNotEmpty()) {
                                    Badge(containerColor = GoldAccent) {
                                        Text(
                                            "${savedVocab.size}",
                                            color = Background,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null)
                        }
                    },
                    label = { Text("Saved") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = GoldAccent,
                        selectedTextColor   = GoldAccent,
                        indicatorColor      = GoldAccent.copy(alpha = 0.12f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TranslateTab(
                modifier         = Modifier.padding(innerPadding),
                inputText        = inputText,
                translationState = translationState,
                toEnglish        = toEnglish,
                isPlaying        = isPlaying,
                savedVocab       = savedVocab,
                apiKey           = apiKey,
                onInputChange    = { viewModel.onInputTextChange(it) },
                onClear          = { viewModel.clearAll() },
                onTranslate      = { if (apiKey.isBlank()) showApiModal = true else viewModel.translate() },
                onSwapDirection  = { viewModel.swapDirection() },
                onSpeak          = { viewModel.speakTranslation() },
                onStop           = { viewModel.stopAudio() },
                onSpeakWord      = { viewModel.speakWord(it) },
                onSaveWord       = { viewModel.saveWord(it) },
                onRemoveWord     = { viewModel.removeWord(it) },
                onOpenSettings   = { showApiModal = true }
            )
            1 -> VocabularyScreen(
                modifier    = Modifier.padding(innerPadding),
                vocabulary  = savedVocab,
                onDismiss   = { selectedTab = 0 },
                onRemove    = { viewModel.removeWord(it) },
                onSpeak     = { viewModel.speakWord(it) }
            )
        }
    }
}

@Composable
fun TranslateTab(
    modifier: Modifier = Modifier,
    inputText: String,
    translationState: TranslationState,
    toEnglish: Boolean,
    isPlaying: Boolean,
    savedVocab: List<VocabularyItem>,
    apiKey: String,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onTranslate: () -> Unit,
    onSwapDirection: () -> Unit,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSaveWord: (VocabularyItem) -> Unit,
    onRemoveWord: (VocabularyItem) -> Unit,
    onOpenSettings: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Deconstruct Chinese",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Menu, contentDescription = "Settings", tint = TextSecondary)
            }
        }

        // Language direction bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSwapDirection() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (toEnglish) "Chinese" else "English",
                    color = BluePrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            IconButton(onClick = onSwapDirection) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap direction", tint = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSwapDirection() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (toEnglish) "English" else "Traditional Chinese",
                    color = BluePrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        HorizontalDivider(color = Divider)

        // Scrollable body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Input panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = {
                            Text(
                                if (toEnglish) "Enter Chinese text" else "Enter English text",
                                color = TextSecondary.copy(alpha = 0.4f),
                                fontSize = 22.sp
                            )
                        },
                        textStyle = TextStyle(fontSize = 22.sp, color = TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 130.dp)
                            .padding(end = 32.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor             = BluePrimary,
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary
                        ),
                        maxLines = 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            translationState is TranslationState.Loading -> Row(
                                modifier = Modifier.padding(end = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(16.dp),
                                    color       = BluePrimary,
                                    strokeWidth = 2.dp
                                )
                                Text("Translating...", color = TextSecondary, fontSize = 14.sp)
                            }
                            inputText.isEmpty() -> TextButton(
                                onClick  = {
                                    val pasted = clipboardManager.getText()?.text.orEmpty()
                                    if (pasted.isNotEmpty()) onInputChange(pasted)
                                },
                                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint     = BluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Paste", color = BluePrimary)
                            }
                            else -> TextButton(
                                onClick  = onTranslate,
                                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    if (apiKey.isBlank()) "Enter API Key" else "Translate",
                                    color = BluePrimary
                                )
                            }
                        }
                    }
                }

                // Clear button — top right corner of input panel
                if (inputText.isNotEmpty()) {
                    IconButton(
                        onClick  = onClear,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint     = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Divider between input and output
            HorizontalDivider(color = Divider, thickness = 2.dp)

            // Output
            AnimatedVisibility(
                visible = translationState is TranslationState.Success || translationState is TranslationState.Error,
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut()
            ) {
                when (val state = translationState) {
                    is TranslationState.Success -> TranslationResultCard(
                        result       = state.result,
                        toEnglish    = toEnglish,
                        isPlaying    = isPlaying,
                        savedVocab   = savedVocab,
                        onSpeak      = onSpeak,
                        onStop       = onStop,
                        onSpeakWord  = onSpeakWord,
                        onSaveWord   = onSaveWord,
                        onRemoveWord = onRemoveWord
                    )
                    is TranslationState.Error -> ErrorCard(state.message)
                    else -> {}
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
