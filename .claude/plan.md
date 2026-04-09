# GIFit — Development Plan

## App Overview
A GIF maker for Android that converts photos into animated GIFs with text overlays, adjustable timing, and sharing support. Built with Jetpack Compose, Hilt, and a custom GIF encoder.

## Completed

### Build & Infrastructure
- Cloned from github.com/howarthTech/GIFit.git
- Configured local build environment (JAVA_HOME, local.properties, junction at C:\GITit)
- Moved Gradle build output to `C:\GITit_build\` to prevent Google Drive sync locking
- Wireless ADB setup for Pixel 9 testing (192.168.50.115)

### UX / Keyboard
- `imePadding()` on main screen Column, FrameEditDialog bottom sheet, and FAB so keyboard never covers input fields

### Text Overlay
- Text overlay rendered in preview (previously only baked into exported GIF)
- Draggable anywhere on the preview area (single finger)
- Pinch-to-scale and twist-to-rotate via `detectTransformGestures` on the full preview area
- Color picker: 8 preset colors (white, black, yellow, orange, red, pink, cyan, green)
- Font selector: Bold, Serif, Mono, Sans
- Reset button to snap text back to center at default size/rotation
- All transform + style settings baked into exported GIF

### Frame Delay
- Global interval slider max raised from 3s → 10s
- Per-frame delay slider in FrameEditDialog also raised to 10s

### Preview / Export
- Share button always visible — if tapped before generation, generates then auto-shares
- Progress shown inline while encoding

### App Icon
- New adaptive icon: white film strip frame + play triangle on purple gradient background

## Remaining Work (Prioritised)

### Quick wins
- **Loop count selector** — currently hardcoded to infinite; add 1×, 3×, 5×, ∞ option
- **Speed presets** — tap buttons (0.5s / 1s / 2s / 5s) above the interval slider

### UX Polish
- **Frame count badge** on photo thumbnails in the home list
- **Duplicate frame to specific position** — currently always inserts right after source

### Bigger Features
- **Sticker / emoji overlay** — place emoji on frames alongside text
- **Undo/redo in preview** — reset text positioning mistakes without using the reset button
- **WebP/APNG export** — requires native NDK library (libwebp); significant effort
