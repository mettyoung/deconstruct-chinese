# PRD: Android Context-Menu Translate Popup (Deconstruct Chinese)

## 1. Introduction / Overview

Add an OS-level Android integration so users can translate selected Chinese text from **any app** without opening Deconstruct Chinese full-screen. When the user selects text, the system text-selection toolbar (and, on ColorOS, the **Translate** submenu) lists **Deconstruct Chinese** as the first translation option. Tapping it launches a **floating popup activity** (translucent, dialog-themed) that shows the translation result — mirroring Google Translate's PROCESS_TEXT context-menu UX. The popup includes an overflow action to open the full app pre-filled with the same text.

Problem solved: today users must copy → switch apps → paste. This adds inline translation comparable to Google Translate, but tuned for Chinese → English with per-character vocabulary breakdown.

## 2. Goals

- Register Deconstruct Chinese on Android's standard `PROCESS_TEXT` extension point so selected text routes to the app from any host app.
- Surface the app inside ColorOS's "Translate" submenu by virtue of being a registered `PROCESS_TEXT` handler.
- Rank the app first (or as early as the host OEM allows) via `android:priority` on the intent-filter.
- Render translation in a **floating popup** (translucent activity + dialog theme) — not full-screen — matching Google Translate's selection-menu popup.
- Provide overflow action to open the full Deconstruct Chinese app pre-filled with the selected text.
- Validate that the selection is Chinese; reject non-Chinese gracefully.
- Universal across stock Android selection menu, ColorOS Translate group, and other OEMs (Samsung, MIUI, OxygenOS) using only standard `PROCESS_TEXT` — no OEM-specific APIs.

## 3. User Stories

### US-001: Add dedicated `TranslatePopupActivity` with floating dialog theme
**Description:** As a developer, I need a separate Android activity that hosts the popup UI so that the existing `MainActivity` keeps its full-screen launcher behavior while context-menu launches open a transparent floating window.

**Acceptance Criteria:**
- [ ] New `TranslatePopupActivity` class in `composeApp/src/androidMain/kotlin/com/mettyoung/deconstructchinese/`.
- [ ] Activity declared in `AndroidManifest.xml` with `android:exported="true"`, `android:launchMode="singleTask"`, `android:excludeFromRecents="true"`, `android:theme="@style/Theme.DeconstructChinese.Popup"`.
- [ ] New theme `Theme.DeconstructChinese.Popup` defined under `composeApp/src/androidMain/res/values/themes.xml` inheriting `Theme.Material3.DayNight.Dialog` (or `Theme.MaterialComponents.Dialog.Alert`), with `windowIsTranslucent=true`, `windowBackground=@android:color/transparent`, `windowNoTitle=true`, `windowIsFloating=true`.
- [ ] Touching outside dismisses the activity (`finish()` via `onTouchOutside` / default dialog behavior).
- [ ] Typecheck + Android build passes (`./gradlew :composeApp:assembleDebug`).

### US-002: Move `PROCESS_TEXT` intent-filter from `MainActivity` to `TranslatePopupActivity` with priority
**Description:** As a user, when I tap "Deconstruct Chinese" from the text-selection menu, the popup activity launches — not the full app.

**Acceptance Criteria:**
- [ ] Remove `PROCESS_TEXT` intent-filter from `MainActivity` in `AndroidManifest.xml`.
- [ ] Add to `TranslatePopupActivity` with `android:label="@string/process_text_label"` and `android:priority="100"`.
- [ ] Intent-filter contains `android.intent.action.PROCESS_TEXT`, `android.intent.category.DEFAULT`, `data android:mimeType="text/plain"`.
- [ ] On a real ColorOS device (or emulator approximation): selecting text in another app → tapping the selection-menu overflow → tapping "Translate" group shows Deconstruct Chinese in the list (first if no other app has a higher priority).
- [ ] Confirm `MainActivity` still launches normally from app launcher and from `SEND` intents (existing shared-text flow unchanged).

### US-003: Read selected text and gate on Chinese-only validation
**Description:** As a user, when I trigger the popup with non-Chinese text, I see a clear message and the popup does not call the translation API.

**Acceptance Criteria:**
- [ ] `TranslatePopupActivity.onCreate` reads `intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)` (and `EXTRA_PROCESS_TEXT_READONLY`).
- [ ] Run Han-character detection (reuse the same regex used by `TranslatorViewModel.onSharedText`, e.g. `Regex("[\\p{IsHan}]")`).
- [ ] If no Han chars present, popup shows error state: title "Not Chinese", body "Deconstruct Chinese only translates Chinese text.", a single "Close" button. No network call.
- [ ] If Han chars present, proceed to translation flow.
- [ ] Empty/null selection → finish activity immediately (no popup flash).

### US-004: Translate inside the popup using existing `QwenService` and render `TranslationResultCard`
**Description:** As a user, I see the translation, pinyin, and per-character vocabulary breakdown inside the popup — identical layout to the in-app result card.

**Acceptance Criteria:**
- [ ] Popup hosts a Compose surface bounded to dialog-appropriate size (e.g. `wrapContentHeight`, `widthIn(max = 340.dp)`, `heightIn(max = 560.dp)`, scrollable column).
- [ ] Direction hardcoded: `toEnglish = true`. `useSimplified` read from `AppSettings`.
- [ ] Use `QwenService.translate(text, toEnglish=true, useSimplified=AppSettings.useSimplified)`.
- [ ] API key sourced from `Secrets` with `AppSettings.apiKey` override (same precedence as main app).
- [ ] Loading state: indeterminate progress + the selected text echoed at top.
- [ ] Success state: reuse `TranslationResultCard` composable from `ui/components/`.
- [ ] If `AppSettings.apiKey` blank AND `Secrets.defaultApiKey` blank, show error per US-006 with action "Set API key" → opens main app.
- [ ] Typecheck passes, Android build passes.
- [ ] Verify in browser/emulator: triggering popup from a host app shows the rendered card.

### US-005: Overflow menu in popup — "Open in app"
**Description:** As a user, I can promote the popup to the full app (with the same text pre-filled) via an overflow menu in the popup's top-right, matching Google Translate's behavior.

**Acceptance Criteria:**
- [ ] Popup header row contains: selected-text title (truncated to 1 line), spacer, overflow icon (`MoreVert`).
- [ ] Tapping overflow shows a `DropdownMenu` with one item: "Open in Deconstruct Chinese".
- [ ] Tapping the item launches `MainActivity` via `Intent` with `ACTION_PROCESS_TEXT` extra carrying the same text (re-uses existing `IncomingText` ingestion path), `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`, then `finish()` the popup.
- [ ] After launching, the main app opens on the Translate screen with the input pre-filled and translation already running (existing behavior of `onSharedText`).
- [ ] Verify in emulator.

### US-006: Inline error state with "Open app" affordance
**Description:** As a user, if the translation fails (network/API key/rate limit), I see the error inside the popup and can jump to the full app to retry or adjust settings.

**Acceptance Criteria:**
- [ ] Map `TranslationState.Error` to popup error UI: error title, message, two buttons: "Close" (dismiss) and "Open in app" (same intent as US-005).
- [ ] No retry button in popup (per spec).
- [ ] Distinguish missing-API-key error: button label changes to "Set API key" — still routes to main app on Translate screen (where `ApiKeyDialog` is available).
- [ ] Verify by toggling airplane mode and triggering popup.

### US-007: Reuse `TranslatorViewModel` translation logic without owning UI state for the main screen
**Description:** As a developer, I want translation logic shared between popup and main app without duplicating networking, debounce, or state machinery.

**Acceptance Criteria:**
- [ ] Either (preferred) extract a thin `TranslatorPopupViewModel` in `commonMain` that depends on `QwenService` + `AppSettings` and exposes `translationState: StateFlow<TranslationState>` and `translate(text: String)`; OR reuse `TranslatorViewModel` with a constructor flag that skips debounce when used from popup.
- [ ] No regression in `TranslatorViewModel` behavior for the main screen (debounce, snackbar flow, recording phase, OCR — all unchanged).
- [ ] Unit/integration tests for the popup VM cover: blank API key → error; non-Chinese input rejected before network call; happy path returns Success.
- [ ] Typecheck + tests pass.

### US-008: ColorOS / OEM verification + priority validation
**Description:** As a QA engineer, I need to confirm the popup appears under the Translate group on ColorOS and within the standard text-selection toolbar on stock Android.

**Acceptance Criteria:**
- [ ] Manual test on a ColorOS device (Oppo/OnePlus): select Chinese text in Chrome/Notes/WeChat → context menu shows Translate group → Deconstruct Chinese listed inside.
- [ ] Manual test on stock Android emulator (API 30+): select Chinese text → overflow menu shows Deconstruct Chinese as a `PROCESS_TEXT` action.
- [ ] Document any OEM where the popup fails to appear (Samsung, MIUI) in the PR description for follow-up.
- [ ] Confirm `android:priority="100"` ranks Deconstruct Chinese before/alongside other registered handlers when present (note: ColorOS may sort alphabetically or by usage — document observed order).

## 4. Functional Requirements

- **FR-1:** The app must declare a new `TranslatePopupActivity` exported, single-task, excluded from recents, themed as a translucent floating dialog.
- **FR-2:** The `PROCESS_TEXT` intent-filter (action `android.intent.action.PROCESS_TEXT`, category `DEFAULT`, mimeType `text/plain`) must be attached to `TranslatePopupActivity` only, with `android:priority="100"` and `android:label="@string/process_text_label"`.
- **FR-3:** `MainActivity` must no longer declare a `PROCESS_TEXT` intent-filter, but must still handle the existing `IncomingText` channel when launched explicitly (e.g. from the popup's overflow action) — preserve existing behavior.
- **FR-4:** On `TranslatePopupActivity.onCreate`, the activity must read `EXTRA_PROCESS_TEXT`. Null/empty → `finish()` without UI flash.
- **FR-5:** The activity must reject text containing no Han characters with an inline "Not Chinese" message and no network call.
- **FR-6:** For valid Chinese text the activity must call `QwenService.translate(text, toEnglish = true, useSimplified = AppSettings.useSimplified)` using the same API-key resolution as the main app (`AppSettings.apiKey` overrides `Secrets.defaultApiKey`).
- **FR-7:** The popup must render the `TranslationResultCard` composable for success (full per-character vocab breakdown).
- **FR-8:** The popup must include a top-right `MoreVert` overflow menu with one entry: "Open in Deconstruct Chinese". Selecting it launches `MainActivity` with `ACTION_PROCESS_TEXT` extra carrying the same text and finishes the popup.
- **FR-9:** Error states must render inline with "Close" + "Open in app" buttons. Missing-API-key variant must use the label "Set API key".
- **FR-10:** Tapping outside the popup (system dialog dismiss) must finish the activity without side effects.
- **FR-11:** The popup must not require any new runtime permissions (no `SYSTEM_ALERT_WINDOW`, no clipboard monitoring).
- **FR-12:** Popup width must be capped (`widthIn(max = 340.dp)`) and content scrollable to handle long vocabulary lists.

## 5. Non-Goals (Out of Scope)

- No persistent floating bubble or `SYSTEM_ALERT_WINDOW` overlay (Google Translate's "Tap to Translate" bubble).
- No clipboard monitoring.
- No iOS counterpart in this PRD (iOS share extension tracked separately).
- No translation in directions other than Chinese → English from the popup (use full app for English → Chinese).
- No audio TTS playback inside the popup.
- No save-word / vocabulary persistence from inside the popup (open in app to save).
- No quick-language-toggle UI inside the popup.
- No analytics or telemetry on context-menu usage.
- No fallback offline translation engine.
- No OEM-specific APIs (no ColorOS SDK, no MIUI SDK).

## 6. Design Considerations

- **Match Google Translate context-menu popup**: translucent activity behind a dialog-style card; dim scrim on background; tap-outside-dismiss; overflow `MoreVert` top-right for "Open in app".
- **Reuse `TranslationResultCard`**: keep vocabulary breakdown visually identical to the main screen.
- **Header row layout**: `[Selected text (1-line truncated)] [spacer] [MoreVert]`. Title font slightly smaller than body to keep emphasis on result card.
- **Loading state**: indeterminate `CircularProgressIndicator` + echoed selected text. No skeleton card.
- **Error icon**: leverage existing `ErrorCard` styling for visual consistency.
- **Theme**: `Theme.DeconstructChinese.Popup` must inherit from a Material3 Dialog-friendly base; ensure dark mode parity.
- **Selected-text label resource**: `process_text_label` is already in `strings.xml` — verify it reads "Deconstruct Chinese" (or the desired short label).

## 7. Technical Considerations

- **Existing wiring**: `MainActivity` currently owns the `PROCESS_TEXT` handler and forwards to `IncomingText.submit`. The popup activity must NOT use `IncomingText` (that channel is for the main UI). Popup runs its own VM and translates directly.
- **Compose in dialog activity**: `setContent { ... }` works in `ComponentActivity` with dialog theme. Confirm window flags don't strip status-bar padding awkwardly.
- **`AppContext`**: `AppContext.set(this)` is required for audio/recognizers in the main app. Popup doesn't use audio/recognizers, but should still call `AppContext.set(applicationContext)` (using application context, not the popup activity) to avoid leaking the dialog activity if any shared common code touches it.
- **API key sharing**: `AppSettings` is backed by Multiplatform Settings → SharedPreferences → already process-wide. No new persistence layer.
- **`QwenService` lifetime**: Construct a fresh instance per popup (lightweight) or inject via a process-wide singleton. Avoid sharing the main app's `QwenService` instance owned by `TranslatorViewModel` (life-cycle mismatch).
- **`android:priority`**: documented to influence cross-app resolution ordering but OEMs (especially ColorOS) may override. Real-device verification required (US-008).
- **`launchMode="singleTask"`**: prevents stacking popups when triggered repeatedly. `excludeFromRecents="true"` keeps the popup out of the recents stack so the floating window doesn't survive as a phantom card.
- **Process death**: popup is ephemeral; no state restore needed. If killed while loading, user re-triggers from the host app.
- **Min SDK**: project min SDK 29 — `PROCESS_TEXT` available since 23, `singleTask` + translucent themes fully supported. No constraint.

## 8. Success Metrics

- Selecting Chinese text in any third-party app → "Deconstruct Chinese" appears in selection toolbar / ColorOS Translate group within 2 seconds of install.
- Popup renders translation in ≤ 3 seconds end-to-end on a typical network (within the Qwen API's normal latency envelope).
- Zero regressions in existing flows: launcher entry, `SEND`-intent shared text, in-app translate, recording, OCR.
- No new permissions surfaced to the user.
- Popup dismiss path (outside-tap, Close button, overflow → Open in app) all return user to the original host app instantly (no Recents pollution).

## 9. Open Questions

- Does ColorOS group third-party `PROCESS_TEXT` apps under its "Translate" submenu by app label match, or does it require a specific category/metadata? Real-device test will confirm; if it requires additional metadata (e.g. `com.coloros.translate.SOURCE`), add a follow-up to extend the intent-filter.
- What is the exact behavior of `android:priority` on ColorOS — does it sort by priority or by recency-of-use? US-008 documents observed behavior.
- Should the popup remember the last-used `useSimplified` choice independently, or always honor `AppSettings.useSimplified`? Current spec: always honor `AppSettings` (single source of truth).
- Should we add a small "Saved" indicator on the popup if the selected word already exists in `VocabularyStore`? Out of scope for v1 — revisit after launch.
- Samsung's selection toolbar exposes only a limited subset of `PROCESS_TEXT` handlers — verify Deconstruct Chinese surfaces there or document as a known limitation.
