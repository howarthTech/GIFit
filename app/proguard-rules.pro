# GIFit ProGuard rules

# Keep the GIF encoder classes since they use byte-level operations
-keep class com.gifit.app.gif.** { *; }

# Keep Parcelize model classes
-keep class com.gifit.app.model.** { *; }

# Keep the foreground encoding service
-keep class com.gifit.app.service.GifEncodingService { *; }

# Coil (usually auto-included, but be explicit)
-dontwarn coil3.**
-keep class coil3.** { *; }

# Reorderable library
-dontwarn sh.calvin.reorderable.**
