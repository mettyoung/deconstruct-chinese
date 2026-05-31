# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Maintenance Rules
- This CLAUDE.md is a living document. After any major architectural change, refactor, or new convention, update the relevant sections immediately.
- When I say “update CLAUDE.md”, revise only the changed parts and keep the file concise.

## Project Overview

**DeconstructChinese** — Kotlin Multiplatform Compose app for Chinese character translation and learning. Targets Android, iOS, Web (JS/WASM). Translates text via Qwen API, stores vocabulary locally with frequency tracking.

### Technology Stack

- **KMP**: Kotlin 2.3, Compose Multiplatform 1.10
- **Network**: Ktor Client 3.0 (OkHttp on Android, Darwin on iOS)
- **State**: ViewModel + StateFlow, Multiplatform Settings for persistence
- **Translation**: Qwen API (qwen-plus model)
- **Build**: Gradle 8.11 with version catalog (libs.versions.toml)
- **Audio**: Platform-specific TTS (Android `TextToSpeech`, iOS `AVSpeechSynthesizer`; web stub)
- **Speech Input**: Hold-to-record via `SpeechRecognizer` expect/actual (Android `android.speech`, iOS `SFSpeechRecognizer`)
- **OCR**: `OcrReader` expect/actual (Android ML Kit text-recognition + chinese, iOS Vision)
- **Image Picker**: `rememberImagePickerLauncher` expect fun (Android ActivityResultContracts, iOS UIImagePickerController)
- **API Key**: `Secrets` expect/actual — Android pulls from `BuildConfig.QWEN_API_KEY`, iOS uses generated source via `generateIosSecrets` Gradle task, Web returns `""` (user enters in-app dialog)

### Target Platforms

| Platform | Min SDK | Target SDK | Details |
|----------|---------|------------|---------|
| Android | 29 | 36 | OkHttp client, Google Play Services on Android |
| iOS | 13+ | Arm64 + SimulatorArm64 | Darwin (native) HTTP client |
| Web | N/A | Modern browsers | JS and WASM targets (audio TTS not implemented) |

## Build Commands

### Android
```bash
# Debug build + install
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Run on connected device/emulator
./gradlew :composeApp:installDebug

# Tests
./gradlew :composeApp:connectedAndroidTest
```

### iOS
Open `/iosApp` in Xcode and run via IDE (KMP bridging through framework in `composeApp/build/` after Gradle sync).

### Web
```bash
# WASM (faster, modern browsers)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# JS (slower, older browser support)
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### Common
```bash
# Shared code tests
./gradlew commonTest

# Full test suite
./gradlew test
```

## Architecture

### State Management (ViewModel Pattern)

**TranslatorViewModel** holds UI state, owns coroutine scope (viewModelScope):
- `translationState`: Current translation (Idle, Loading, Success, Error — sealed class)
- `inputText`: User input with 800ms debounce before translate
- `toEnglish`: Direction toggle
- `useSimplified`: Traditional vs simplified preference
- `savedVocabulary`: StateFlow from VocabularyStore
- `isPlaying`: Audio playback status
- `recordingPhase`: `RecordingPhase` enum (`Idle`/`Armed`/`Listening`) driven by `SpeechRecognizer.results` flow
- `isProcessingImage`: OCR in-progress state
- `snackbarMessage`: SharedFlow for speech/OCR errors (non-fatal, shown as snackbar)
- `onSharedText(String)`: entry point from `IncomingText` bus; auto-sets direction by detecting Han chars
- `startRecording()` / `stopRecording()`: wraps SpeechRecognizer; locale derived from `toEnglish` + `useSimplified`
- `processImage(ByteArray)`: runs OcrReader, sets inputText on success

`TranslationState` sealed class lives in `model/TranslationResult.kt` alongside `TranslationResult`, `VocabularyItem`, `Language`.

ViewModel created once per app lifecycle; state flows collected in Compose via `collectAsStateWithLifecycle()`.

### Data Layer

**VocabularyStore** (object singleton) — source of truth for saved words:
- Loads/saves from Multiplatform Settings (SharedPreferences on Android, UserDefaults on iOS, localStorage on Web)
- Sorts by frequency (highest first)
- `saveWord()`: Prevents duplicates via word match
- `bumpFrequency()`: Increments frequency when already-saved word appears in translation
- Exposes `savedVocabulary: StateFlow<List<VocabularyItem>>`

**AppSettings** — typed preferences wrapper:
- `useSimplified`: Boolean — traditional vs simplified preference
- `apiKey`: String — falls back to platform `defaultApiKey` when unset; user override persists
- Backed by Multiplatform Settings

**IncomingText** — `Channel<String>(CONFLATED)` bus for text handed in from outside the app (Android `ACTION_PROCESS_TEXT`/`SEND` intents, iOS share extension via URL scheme). `submitSharedText(text)` is exposed for Swift. `TranslatorRoute` collects `IncomingText.texts` and forwards to `viewModel.onSharedText()`.

**ChineseScriptConverter** (in `util/`) — character-level Simplified↔Traditional mapping (~400 pairs, OpenCC-derived). Unknown chars pass through. Used for client-side display normalization; Qwen still does the authoritative conversion in the JSON response.

### Network

**QwenService** — HTTP client for Qwen API:
- Single shared HttpClient across app lifetime (pooled connections)
- Serializes/deserializes with kotlinx.serialization
- Platform-specific HTTP engines injected via sourceSets (OkHttp/Darwin/Browser default)
- `translate(text, toEnglish, useSimplified)` returns `TranslationResult` with vocabulary list

**Error handling**: Network exceptions caught in ViewModel and mapped to user-friendly `TranslationState.Error` messages (auth, rate limit, connectivity, etc.).

### UI Layer

**App.kt** — thin wrapper: theme + `TranslatorRoute` with `apiKey` state from `AppSettings`.

**TranslatorRoute** (`ui/screens/`) — owns `TranslatorViewModel` (re-created via `key(apiKey)` so a new key rebuilds `QwenService`), wires snackbar host, `IncomingText` collector, image picker, and the bottom-`NavigationBar` Scaffold across all platforms (Translate / Saved tabs).

**TranslateScreen** — Input, translation display, vocab actions:
- Debounced input (800ms delay before API call)
- Shows TranslationResult card with original, translation, pinyin, vocabulary breakdown
- Vocab cards show save/remove buttons and frequency badges

**VocabularyScreen** — Saved words list, frequency sorting

**Components** (`ui/components/`): TranslationResultCard, VocabularyCard, ErrorCard, InputPanel, MicButton, LanguageDirectionBar, ImageSourceDialog, ApiKeyDialog, SectionLabel.

### Platform-Specific (expect/actual)

**AudioPlayer** (`audio/`):
- `speak(text, language)`, `stop()`, `release()`
- Android: `android.speech.tts.TextToSpeech`, initialized on first construct, locale set per `speak`
- iOS: `AVSpeechSynthesizer` (forces `zh-CN` voice, rate 0.45)
- Web: empty stub

**SpeechRecognizer** (`speech/`) — emits `SpeechResult` (`Ready` / `SpeechStarted` / `Partial` / `Final` / `Cancelled` / `Error`); ViewModel maps these to `RecordingPhase` transitions.

**OcrReader** (`ocr/`) — `recognizeText(bytes, OcrLanguage)` returns a `Flow<OcrResult>` (`Success`/`Error`). Android uses ML Kit's Chinese + Latin recognizers; iOS uses Vision.

**ImagePickerLauncher** (`ui/`) — `rememberImagePickerLauncher { bytes -> }`; Android opens gallery via `ActivityResultContracts`, iOS via `UIImagePickerController`.

**AppContext** (Android, in `audio/` package): holds `applicationContext`; set from `MainActivity.onCreate`. Required because `AudioPlayer`/recognizers are constructed from common code.

**`webMain` sourceSet** — intermediate parent of `jsMain` + `wasmJsMain` (wired via the default hierarchy template + matching `src/webMain` directory). Hosts no-op stubs for AudioPlayer, SpeechRecognizer, OcrReader, ImagePickerLauncher, plus `isWebPlatform = true`.

## Key Design Decisions

1. **ViewModel in common**: AndroidX ViewModel is multiplatform-compatible (via lifecycle-viewmodel-compose); used in all platforms for consistency.

2. **Frequency tracking cross-script**: Vocabulary items matched by word OR (phonetic + meaning) to handle traditional/simplified variants with shared frequency.

3. **No manual JSON**: kotlinx.serialization with `@Serializable` on all data classes; Ktor handles JSON automatically.

4. **No exceptions for expected failures**: Translation/network errors are modeled as `TranslationState.Error` sealed class variant, not thrown.

5. **Multiplatform Settings over platform-specific**: Unified persistence API; serialization plugin for complex types (List<VocabularyItem>).

6. **API key injection diverges by platform**: Android via `BuildConfig` (build.gradle reads `qwen.apiKey` from `local.properties` or `QWEN_API_KEY` env var); iOS via the `generateIosSecrets` task that writes `SecretsGenerated.kt` into `iosMain` build output; Web intentionally has empty default (would leak in JS bundle). All platforms persist a user-supplied override in `AppSettings.apiKey`.

## Common Workflows

### Adding a new user preference
1. Add field to `AppSettings` with getter/setter
2. Expose in ViewModel as `StateFlow`
3. Collect in UI and pass to composables
4. Preference persists automatically via Multiplatform Settings

### Fixing a translation issue
1. Check `QwenService.translate()` prompt logic
2. Verify `TranslationResult` data class matches API response
3. Add error case to `ViewModel.translate()` catch block if needed
4. Test via Android debug build (fastest iteration)

### Adding platform-specific feature (e.g., iOS-only gesture)
1. Create `expect` interface in commonMain
2. Add `actual` in iosMain with native API calls
3. Use from common code (no conditional imports)

## Dependencies

- **Compose**: Material3 with extended icons
- **HTTP**: Ktor client (platform engines auto-selected)
- **Serialization**: kotlinx.serialization (JSON)
- **Coroutines**: kotlinx.coroutines (Main dispatcher implicit in ViewModel)
- **Persistence**: Multiplatform Settings (with serialization plugin)
- **Testing**: JUnit 4, Espresso (minimal test suite currently)

Add new deps to `libs.versions.toml` version catalog only; do not hardcode versions in build.gradle.kts.

## Notes

- **No strict null safety for API responses**: QwenService uses lenient JSON parsing; malformed responses logged but non-fatal. Markdown fences (```json``` / ``` ```) are stripped before parsing.
- **Audio resource cleanup**: `TranslatorViewModel.onCleared()` releases `AudioPlayer` and `SpeechRecognizer`.
- **iOS framework**: Gradle builds framework binary to `composeApp/build/XCFramework/` after Kotlin compilation; Xcode links it.
- **Web audio stub**: TTS not implemented for JS/WASM; UI gracefully hides audio buttons on web.
- **Shared text entry**: Android `ACTION_PROCESS_TEXT` intent in `MainActivity.handleSharedText` submits to `IncomingText`. iOS share extension is currently reverted (see commit `fd42978`); `submitSharedText()` remains in commonMain for re-introduction.
- **Android-only permissions** (`AndroidManifest.xml`): `INTERNET`, `RECORD_AUDIO`, `CAMERA`. Launcher activity is `singleTop` to keep PROCESS_TEXT intents on the existing instance.
