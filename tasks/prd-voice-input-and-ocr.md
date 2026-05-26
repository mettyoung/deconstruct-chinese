# PRD: Voice Input and OCR to Text

## Introduction

Add two alternative text-input methods to the Translate tab: hold-to-record voice input (speech-to-text) and image-based OCR (camera or gallery). Both capture methods populate the input field and trigger auto-translation immediately. Implemented via expect/actual with native platform APIs on Android and iOS.

## Goals

- Let users speak Chinese or English directly into the translate input
- Let users photograph or select an image containing Chinese/English text and extract it
- Both inputs auto-translate without extra taps
- Zero third-party SDKs beyond what platform provides natively (ML Kit for Android OCR via Play Services, Speech + Vision frameworks on iOS)
- Graceful permission handling on both platforms

## User Stories

### US-001: Define expect/actual SpeechRecognizer interface
**Description:** As a developer, I need a common interface for speech recognition so that the UI layer calls platform-agnostic code.

**Acceptance Criteria:**
- [ ] `expect class SpeechRecognizer` in `commonMain` with `startListening(locale: String)`, `stopListening()`, `release()`, and a `results: Flow<SpeechResult>` (sealed: `Partial`, `Final`, `Error`)
- [ ] Stub `actual` in `webMain` that emits `SpeechResult.Error("unsupported")`
- [ ] Typecheck passes

### US-002: Android SpeechRecognizer actual implementation
**Description:** As a developer, I need the Android actual using `android.speech.SpeechRecognizer` so that voice input works on Android.

**Acceptance Criteria:**
- [ ] `actual class SpeechRecognizer` in `androidMain` wraps `android.speech.SpeechRecognizer`
- [ ] `RECORD_AUDIO` permission declared in `AndroidManifest.xml`
- [ ] Emits `SpeechResult.Partial` during recognition, `SpeechResult.Final` on completion
- [ ] Emits `SpeechResult.Error` on permission denial or recognition failure
- [ ] `release()` calls `destroy()` on underlying recognizer
- [ ] Typecheck passes

### US-003: iOS SpeechRecognizer actual implementation
**Description:** As a developer, I need the iOS actual using `Speech` framework so that voice input works on iOS.

**Acceptance Criteria:**
- [ ] `actual class SpeechRecognizer` in `iosMain` uses `SFSpeechRecognizer` + `SFSpeechAudioBufferRecognitionRequest` + `AVAudioEngine`
- [ ] `NSMicrophoneUsageDescription` + `NSSpeechRecognitionUsageDescription` added to `Info.plist`
- [ ] Requests both permissions on first use; emits `SpeechResult.Error` if denied
- [ ] Emits `SpeechResult.Final` when audio engine stops
- [ ] `release()` stops engine and cancels recognition task
- [ ] Typecheck passes

### US-004: Hold-to-record mic button in TranslateTab UI
**Description:** As a user, I want to hold a mic button to record my speech so that I can speak instead of type.

**Acceptance Criteria:**
- [ ] Mic `IconButton` added to TranslateTab input row (alongside existing controls)
- [ ] Press-and-hold starts recording; release stops recording
- [ ] Visual indicator (pulsing animation or tint change) active while recording
- [ ] `isRecording: Boolean` state in `TranslatorViewModel`
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill (Android emulator for voice)

### US-005: Wire speech result to input field and auto-translate
**Description:** As a user, I want my spoken words to appear in the input and translate automatically so that I don't need extra taps.

**Acceptance Criteria:**
- [ ] `SpeechResult.Final` text sets `inputText` in `TranslatorViewModel`
- [ ] Setting `inputText` triggers existing 800ms debounce → auto-translation
- [ ] `SpeechResult.Error` shows snackbar with error message (not `TranslationState.Error`)
- [ ] `SpeechRecognizer.release()` called in `ViewModel.onCleared()`
- [ ] Typecheck passes

### US-006: Define expect/actual OcrReader interface
**Description:** As a developer, I need a common interface for OCR so that the UI layer is platform-agnostic.

**Acceptance Criteria:**
- [ ] `expect class OcrReader` in `commonMain` with `recognizeText(imageBytes: ByteArray): Flow<OcrResult>` (sealed: `Success(text: String)`, `Error(message: String)`)
- [ ] Stub `actual` in `webMain` that emits `OcrResult.Error("unsupported")`
- [ ] Typecheck passes

### US-007: Android OcrReader actual implementation (ML Kit)
**Description:** As a developer, I need the Android actual using ML Kit Text Recognition so that OCR works on Android.

**Acceptance Criteria:**
- [ ] `actual class OcrReader` in `androidMain` uses `com.google.mlkit:text-recognition` (add to `libs.versions.toml`)
- [ ] `recognizeText()` converts `ByteArray` → `InputImage`, runs ML Kit, emits `OcrResult.Success` with extracted text or `OcrResult.Error`
- [ ] ML Kit dependency uses on-device model (no network call)
- [ ] Typecheck passes

### US-008: iOS OcrReader actual implementation (Vision framework)
**Description:** As a developer, I need the iOS actual using the Vision framework so that OCR works on iOS.

**Acceptance Criteria:**
- [ ] `actual class OcrReader` in `iosMain` uses `VNRecognizeTextRequest` from `Vision` framework
- [ ] `recognizeText()` converts `ByteArray` → `UIImage` → `VNImageRequestHandler`, runs request, emits `OcrResult.Success` or `OcrResult.Error`
- [ ] Recognition level set to `.accurate`
- [ ] Typecheck passes

### US-009: Image picker UI in TranslateTab (camera + gallery)
**Description:** As a user, I want to tap a camera button to choose an image from my camera or gallery so that I can extract text from a photo.

**Acceptance Criteria:**
- [ ] Camera `IconButton` added to TranslateTab input row
- [ ] Tapping shows bottom sheet or dialog: "Take Photo" / "Choose from Gallery"
- [ ] Android: uses `ActivityResultContracts.TakePicture` and `ActivityResultContracts.GetContent`
- [ ] iOS: uses `UIImagePickerController` via expect/actual or KMP-compatible wrapper
- [ ] Required permissions declared: `CAMERA` in manifest (Android); `NSCameraUsageDescription` + `NSPhotoLibraryUsageDescription` in `Info.plist` (iOS)
- [ ] Selected image passed as `ByteArray` to `TranslatorViewModel.processImage()`
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill (Android emulator for camera/gallery)

### US-010: Wire OCR result to input field and auto-translate
**Description:** As a user, I want extracted text to appear in the input and translate automatically so that I don't need extra taps.

**Acceptance Criteria:**
- [ ] `OcrResult.Success` text sets `inputText` in `TranslatorViewModel`
- [ ] Setting `inputText` triggers existing 800ms debounce → auto-translation
- [ ] `OcrResult.Error` shows snackbar with error message
- [ ] `isProcessingImage: Boolean` state in ViewModel; loading indicator shown on camera button while processing
- [ ] `OcrReader.release()` (if applicable) called in `ViewModel.onCleared()`
- [ ] Typecheck passes

## Functional Requirements

- FR-1: Hold-to-record mic button in TranslateTab; release ends recording
- FR-2: `SpeechRecognizer` expect/actual: `android.speech.SpeechRecognizer` on Android, `SFSpeechRecognizer` on iOS, stub on Web
- FR-3: Speech result (final text) sets `inputText` and auto-translates via existing debounce
- FR-4: Camera icon button in TranslateTab opens "Take Photo / Choose from Gallery" picker
- FR-5: `OcrReader` expect/actual: ML Kit on Android, `VNRecognizeTextRequest` on iOS, stub on Web
- FR-6: OCR result text sets `inputText` and auto-translates via existing debounce
- FR-7: Both features emit typed error results (not exceptions); errors shown as snackbar
- FR-8: All required permissions declared in platform manifests; requested at runtime on first use
- FR-9: `SpeechRecognizer` and `OcrReader` released in `ViewModel.onCleared()`

## Non-Goals

- Web platform support for voice or OCR (stubs only, no UI buttons shown on Web)
- Real-time live OCR / viewfinder overlay
- Partial/streaming speech display in input field (only final result populates)
- Manual translation trigger after voice/OCR (auto-translate is the only flow)
- Cloud-based OCR (on-device only)
- Offline speech recognition (platform default behavior applies)

## Design Considerations

- Mic and camera buttons sit in the same row as existing input controls in `TranslateTab`
- While recording: mic button pulses or changes tint (e.g. `GoldAccent`)
- While processing OCR: camera button shows `CircularProgressIndicator`
- Buttons hidden on Web (conditional on platform)
- Reuse existing `ErrorCard` or snackbar pattern for errors; do not repurpose `TranslationState.Error`

## Technical Considerations

- `SpeechRecognizer` and `OcrReader` injected into `TranslatorViewModel` constructor (not singletons); created via platform factory or `remember`
- Image picker on iOS requires UIKit interop — wrap in `expect/actual` (`ImagePickerLauncher`) to keep commonMain clean
- ML Kit: add `com.google.mlkit:text-recognition` to `androidMain` dependencies in `libs.versions.toml`; not available in common or iOS
- Vision framework: available via Kotlin/Native ObjC interop, no extra dependency
- `ByteArray` as image interchange type keeps `OcrReader` interface platform-neutral
- Android camera/gallery uses Compose `rememberLauncherForActivityResult`; result callback in composable passes bytes to ViewModel

## Success Metrics

- User can speak and see translation in under 3 seconds (network latency excluded)
- User can photograph text and see translation in under 5 seconds
- No crash on permission denial on either platform
- No regression in existing keyboard-input translate flow

## Open Questions

- Should partial speech results display in input field as live preview (currently out of scope)?
- iOS image picker: use `PHPickerViewController` (iOS 14+) instead of `UIImagePickerController`? PHPicker is recommended but requires more interop boilerplate.
- Should OCR attempt to detect language automatically or always use current `toEnglish` toggle direction?
- Multi-column or multi-block OCR text — join with newlines or spaces?
