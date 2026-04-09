# GIF.it — SDK Compatibility Update Plan

## Current State
The project is already a fully functional GIF maker with:
- Photo picker, drag-to-reorder, interval slider, GIF preview
- Custom GIF encoder (NeuQuant + LZW)
- Save to gallery & share
- Proper version-gated storage (MediaStore on API 29+, legacy file I/O below)

## What Needs to Change

### 1. Update `app/build.gradle.kts` SDK versions
- `compileSdk`: 35 → **36** (Android 16)
- `targetSdk`: 35 → **36**
- `minSdk`: 24 → **21** (Android 5.0, ~99% device coverage)

### 2. Update AGP version (if needed)
- Current AGP: **8.7.3** — may not support `compileSdk 36`
- Will update `build.gradle.kts` root plugin to a version that supports API 36 (e.g. 8.9.x+)
- Update Kotlin & Compose plugin versions if required for compatibility

### 3. Verify API 21 compatibility across all source files
The code already looks safe:
- `MediaStoreSaver.kt` — version-gates API 29+ MediaStore vs legacy file I/O ✅
- `Theme.kt` — version-gates dynamic color (API 31+) ✅
- `PreviewScreen.kt` — version-gates storage permission request ✅
- `PickMultipleVisualMedia` — backported via AndroidX Activity, works on API 21+ ✅
- Jetpack Compose supports minSdk 21 ✅

No code changes expected beyond build config.

### 4. Build & verify
- Run `./gradlew assembleDebug` to confirm everything compiles cleanly

## Files to Edit
1. `/home/darrell/Desktop/GIFit/build.gradle.kts` — AGP/Kotlin plugin versions
2. `/home/darrell/Desktop/GIFit/app/build.gradle.kts` — compileSdk, targetSdk, minSdk
