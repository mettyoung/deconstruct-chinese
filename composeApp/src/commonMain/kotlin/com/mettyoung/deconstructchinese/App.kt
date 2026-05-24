package com.mettyoung.deconstructchinese

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.mettyoung.deconstructchinese.ui.ImagePickerLauncher
import com.mettyoung.deconstructchinese.ui.components.ErrorCard
import com.mettyoung.deconstructchinese.ui.components.TranslationResultCard
import com.mettyoung.deconstructchinese.ui.rememberImagePickerLauncher
import com.mettyoung.deconstructchinese.ui.screens.VocabularyScreen
import com.mettyoung.deconstructchinese.ui.theme.*
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
            surfaceVariant = Card
        ),
        typography = Typography().copy(
            headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
        )
    ) {
        var apiKey by rememberSaveable { mutableStateOf("sk-043c8d868fed44758bb76d84774aeeea") }
        TranslatorScreen(apiKey = apiKey, onApiKeySubmit = { apiKey = it })
    }
}

@Composable
fun ApiKeyModal(
    currentApiKey: String,
    useSimplified: Boolean,
    onDismiss: () -> Unit,
    onApiKeySubmit: (String) -> Unit,
    onUseSimplifiedChange: (Boolean) -> Unit
) {
    var keyInput by remember { mutableStateOf(currentApiKey) }
    var showKey  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Card)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Chinese Script", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (useSimplified) "Simplified (简体)" else "Traditional (繁體)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = useSimplified,
                        onCheckedChange = onUseSimplifiedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BluePrimary
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "API Key",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("sk-...", color = TextSecondary.copy(alpha = 0.5f)) },
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BluePrimary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeySubmit(keyInput.trim()); onDismiss() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.height(48.dp).fillMaxWidth(0.4f)
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

    val inputText          by viewModel.inputText.collectAsStateWithLifecycle()
    val translationState   by viewModel.translationState.collectAsStateWithLifecycle()
    val isPlaying          by viewModel.isPlaying.collectAsStateWithLifecycle()
    val savedVocab         by viewModel.savedVocabulary.collectAsStateWithLifecycle()
    val toEnglish          by viewModel.toEnglish.collectAsStateWithLifecycle()
    val useSimplified      by viewModel.useSimplified.collectAsStateWithLifecycle()
    val isRecording        by viewModel.isRecording.collectAsStateWithLifecycle()
    val isProcessingImage  by viewModel.isProcessingImage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val imagePicker = rememberImagePickerLauncher { bytes ->
        viewModel.processImage(bytes)
    }

    var showApiModal by remember { mutableStateOf(false) }
    var selectedTab  by rememberSaveable { mutableIntStateOf(0) }

    if (showApiModal) {
        ApiKeyModal(
            currentApiKey = apiKey,
            useSimplified = useSimplified,
            onDismiss = { showApiModal = false },
            onApiKeySubmit = onApiKeySubmit,
            onUseSimplifiedChange = { viewModel.setUseSimplified(it) }
        )
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                tonalElevation = 8.dp,
                modifier = Modifier.shadow(16.dp)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                    label = { Text("Translate") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = BluePrimary,
                        selectedTextColor   = BluePrimary,
                        indicatorColor      = BluePrimary.copy(alpha = 0.1f),
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
                                    Badge(
                                        containerColor = GoldAccent,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            "${savedVocab.size}",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
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
                        indicatorColor      = GoldAccent.copy(alpha = 0.1f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TranslateTab(
                    inputText        = inputText,
                    translationState = translationState,
                    toEnglish        = toEnglish,
                    isPlaying        = isPlaying,
                    isRecording      = isRecording,
                    isProcessingImage = isProcessingImage,
                    savedVocab       = savedVocab,
                    apiKey           = apiKey,
                    useSimplified    = useSimplified,
                    imagePicker      = imagePicker,
                    onInputChange    = { viewModel.onInputTextChange(it) },
                    onClear          = { viewModel.clearAll() },
                    onTranslate      = { if (apiKey.isBlank()) showApiModal = true else viewModel.translate() },
                    onSwapDirection  = { viewModel.swapDirection() },
                    onSpeak          = { viewModel.speakTranslation() },
                    onStop           = { viewModel.stopAudio() },
                    onSpeakWord      = { viewModel.speakWord(it) },
                    onSaveWord       = { viewModel.saveWord(it) },
                    onRemoveWord     = { viewModel.removeWord(it) },
                    onOpenSettings   = { showApiModal = true },
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecording  = { viewModel.stopRecording() }
                )
                1 -> VocabularyScreen(
                    vocabulary   = savedVocab,
                    useSimplified = useSimplified,
                    onDismiss    = { selectedTab = 0 },
                    onRemove     = { viewModel.removeWord(it) },
                    onSpeak      = { viewModel.speakWord(it) }
                )
            }
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
    isRecording: Boolean,
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
    val clipboardManager = LocalClipboardManager.current
    var showImageSourceDialog by remember { mutableStateOf(false) }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Add Image", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showImageSourceDialog = false
                                imagePicker.launchCamera()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = BluePrimary)
                        Text("Take Photo", fontSize = 16.sp)
                    }
                    HorizontalDivider(color = Divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showImageSourceDialog = false
                                imagePicker.launchGallery()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BluePrimary)
                        Text("Choose from Gallery", fontSize = 16.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic")
    val micPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Deconstruct",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    "Chinese",
                    style = MaterialTheme.typography.headlineSmall.copy(color = BluePrimary)
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Card)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
            }
        }

        // Language direction bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chineseLabel = if (useSimplified) "Simplified" else "Traditional"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSwapDirection() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (toEnglish) chineseLabel else "English",
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = onSwapDirection,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Background)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = BluePrimary)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSwapDirection() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (toEnglish) "English" else chineseLabel,
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Scrollable body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Input panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box {
                        TextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            placeholder = {
                                Text(
                                    if (toEnglish) "Type or paste Chinese..." else "Type English text...",
                                    color = TextSecondary.copy(alpha = 0.4f),
                                    fontSize = 20.sp
                                )
                            },
                            textStyle = TextStyle(fontSize = 20.sp, color = TextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor             = BluePrimary
                            )
                        )

                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick  = onClear,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: paste / mic / camera
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            }

                            if (!isWebPlatform) {
                                // Mic hold-to-record button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isRecording) GoldAccent.copy(alpha = micPulseAlpha)
                                            else Color.Transparent
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(onPress = { _ ->
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

                                // Camera button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isProcessingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = BluePrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { showImageSourceDialog = true },
                                            enabled = !isRecording
                                        ) {
                                            Icon(
                                                Icons.Default.CameraAlt,
                                                contentDescription = "Scan image",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right side: translate / loading
                        if (translationState is TranslationState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(end = 8.dp),
                                color = BluePrimary,
                                strokeWidth = 2.dp
                            )
                        } else if (inputText.isNotEmpty() && translationState !is TranslationState.Success) {
                            Button(
                                onClick = onTranslate,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(if (apiKey.isBlank()) "Setup API" else "Translate", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Output
            AnimatedVisibility(
                visible = translationState is TranslationState.Success || translationState is TranslationState.Error,
                enter   = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { it / 2 }),
                exit    = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    when (val state = translationState) {
                        is TranslationState.Success -> TranslationResultCard(
                            result       = state.result,
                            toEnglish    = toEnglish,
                            isPlaying    = isPlaying,
                            savedVocab   = savedVocab,
                            useSimplified = useSimplified,
                            onSpeak      = onSpeak,
                            onStop       = onStop,
                            onSpeakWord  = onSpeakWord,
                            onSaveWord   = onSaveWord,
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
