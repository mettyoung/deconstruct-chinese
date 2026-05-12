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
- **Audio**: Platform-specific TTS (Android MediaPlayer, iOS AVFoundation expect/actual)

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

ViewModel created once per app lifecycle; state flows collected in Compose via `collectAsStateWithLifecycle()`.

### Data Layer

**VocabularyStore** (object singleton) — source of truth for saved words:
- Loads/saves from Multiplatform Settings (SharedPreferences on Android, UserDefaults on iOS, localStorage on Web)
- Sorts by frequency (highest first)
- `saveWord()`: Prevents duplicates via word match
- `bumpFrequency()`: Increments frequency when already-saved word appears in translation
- Exposes `savedVocabulary: StateFlow<List<VocabularyItem>>`

**AppSettings** — typed preferences wrapper:
- Currently: `useSimplified` (traditional vs simplified preference)
- Backed by Multiplatform Settings

### Network

**QwenService** — HTTP client for Qwen API:
- Single shared HttpClient across app lifetime (pooled connections)
- Serializes/deserializes with kotlinx.serialization
- Platform-specific HTTP engines injected via sourceSets (OkHttp/Darwin/Browser default)
- `translate(text, toEnglish, useSimplified)` returns `TranslationResult` with vocabulary list

**Error handling**: Network exceptions caught in ViewModel and mapped to user-friendly `TranslationState.Error` messages (auth, rate limit, connectivity, etc.).

### UI Layer

**App.kt** — Root composable with theme, navigation, ViewModel factory:
- Material3 theme (custom colors: BluePrimary, GoldAccent, etc.)
- Scaffold with bottom NavigationBar (Android) or inline tabs (iOS/Web)
- Two main tabs: Translate + Vocabulary

**TranslateTab** — Input, translation display, vocab actions:
- Debounced input (800ms delay before API call)
- Shows TranslationResult card with original, translation, pinyin, vocabulary breakdown
- Vocab cards show save/remove buttons and frequency badges

**VocabularyScreen** — Saved words list, frequency sorting

**Components**: TranslationResultCard, VocabularyCard, ErrorCard (reusable across screens)

### Platform-Specific (expect/actual)

**AudioPlayer** (expect in commonMain, actual in androidMain/iosMain):
- `speak(text, language)` — TTS for pinyin/translation
- `stop()` — Cancel playback
- `release()` — Cleanup resources
- Android: MediaPlayer via TextToSpeech
- iOS: AVFoundation AVSpeechSynthesizer
- Web: Stub (empty implementations)

**AppContext** (Android only):
- Holds global Android Context for platform APIs

## Key Design Decisions

1. **ViewModel in common**: AndroidX ViewModel is multiplatform-compatible (via lifecycle-viewmodel-compose); used in all platforms for consistency.

2. **Frequency tracking cross-script**: Vocabulary items matched by word OR (phonetic + meaning) to handle traditional/simplified variants with shared frequency.

3. **No manual JSON**: kotlinx.serialization with `@Serializable` on all data classes; Ktor handles JSON automatically.

4. **No exceptions for expected failures**: Translation/network errors are modeled as `TranslationState.Error` sealed class variant, not thrown.

5. **Multiplatform Settings over platform-specific**: Unified persistence API; serialization plugin for complex types (List<VocabularyItem>).

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

- **No strict null safety for API responses**: QwenService uses lenient JSON parsing; malformed responses logged but non-fatal.
- **Audio resource cleanup**: Call `audioPlayer.release()` in ViewModel.onCleared() or screen disposal.
- **iOS framework**: Gradle builds framework binary to `composeApp/build/XCFramework/` after Kotlin compilation; Xcode links it.
- **Web audio stub**: TTS not implemented for JS/WASM; UI gracefully hides audio buttons on web.
