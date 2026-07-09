# GIF.it — Project Plan & State

## Overview
Android app that turns a sequence of user photos into an animated GIF.
- **Language/UI:** Kotlin + Jetpack Compose (Material 3)
- **Min SDK:** 21 (Android 5.0) — ~99% device coverage
- **Target/Compile SDK:** 36 (Android 16)
- **Package:** `com.gifit.app`

## Architecture
- `MainActivity` → `GIFitApp` (screen state + animated transitions)
- Screens: `HomeScreen` (pick/edit photos, settings) → `PreviewScreen` (animated preview + export)
- ViewModels: `HomeViewModel`, `PreviewViewModel` (Hilt-injected, SavedStateHandle for persistence)
- Models: `PhotoFrame`, `GifSettings`, `PhotoFrameState`, `QuantizerType`, `ResolutionPreset`
- GIF engine: `AnimatedGifEncoder` + quantizers (`NeuQuantQuantizer`, `MedianCutQuantizer`) + `LzwEncoder` + `GifWriter`
- Utils: `ImageResizer` (EXIF-aware, downsample), `MediaStoreSaver` (MediaStore on API 29+, legacy file I/O below)

## Implemented Features

### Photo Management
- Multi-photo picker (Android PhotoPicker)
- Drag-to-reorder (reorderable library)
- Per-frame edits: **rotate 90°**, **flip horizontal**, **flip vertical**
- Per-frame crop rect (normalized coords)
- Duplicate frame
- Remove frame
- Undo/redo history (up to 20 states)

### GIF Settings
- Global frame delay (interval slider, seconds)
- Per-frame delay override
- Global overlay text (rendered across entire GIF)
- Per-frame overlay text override
- Resolution preset selection
- Quantizer selection (NeuQuant / MedianCut)

### Preview Screen
- Live animated preview (cycles frames at configured delay)
- GIF generation with progress reporting
- Save to gallery ("Pictures/GIFit")
- Share intent
- File size + frame count display
- Error surfacing via StateFlow

### Persistence
- `SavedStateHandle` keeps frames + settings across process death

### Polish
- Custom app icon (film strip + play triangle + "GIF" text, adaptive icon support)
- Haptic feedback
- Animated screen transitions (slide/fade)
- Dark theme support (Material You dynamic color on API 31+)

## GIF Encoder Notes
- Global color palette built from sampled pixels across all frames
- Per-frame LZW compression
- Netscape 2.0 extension for infinite loop
- Graphic Control Extension carries per-frame delay (centiseconds)
- ProGuard rules preserve encoder classes for byte-level ops

## Build
```
export JAVA_HOME=/snap/android-studio/209/jbr
export ANDROID_HOME=/home/darrell/Android/Sdk
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`
Convenience copy: `/home/darrell/Desktop/GIFit-debug.apk`

## Known / Potential Follow-ups
- Unit/instrumentation tests (test dirs exist but empty)
- Transition animations between individual photo edits
- Larger overlay-text styling options (font, color, position)

## Branch Reconciliation & Play Store Prep (2026-07-09)
`main` and a since-abandoned `master` branch had diverged since 2026-06-13, each independently
building an overlay-text system and each independently doing a "remove dead service / add
signing" cleanup. `main` was kept as the base (more mature: per-frame `TextOverlayStyle`,
`FrameEditorSheet`, mixed-dimension encoder crash fix, dithering, transitions). Ported forward
from `master` on top of `main`:
- App icon redesign (white film-strip + play triangle on purple gradient)
- `imePadding()` keyboard fix on `HomeScreen`
- Root `build.gradle.kts` build-output redirect (`C:/GITit_build`) to avoid Google Drive sync locks
- Delay sliders (`IntervalSlider`, `FrameEditorSheet`) bumped from 3.0s/28 steps to 10.0s/98 steps

Added net-new on top of `main` for Play Store readiness:
- `isShrinkResources = true` alongside existing R8 minification for release builds
- `OutOfMemoryError` handling around image load / GIF generate in `PreviewViewModel`, with a
  friendly error message instead of a crash
- `strings.xml` (`app_name`), manifest label now references it
- `PRIVACY_POLICY.md` + `docs/privacy-policy.html` (styled page for GitHub Pages hosting)

`main` already had (as of commit `a1ffba2`, 2026-06-13): dead `GifEncodingService` + its 3
permissions removed, and a release `signingConfig` reading `keystore.properties`/env vars with a
debug-key fallback. See `STATUS.md` for the Play Console punch list.
