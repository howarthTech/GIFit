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
- Signed release build (currently only debug)
- Transition animations between individual photo edits
- Larger overlay-text styling options (font, color, position)
