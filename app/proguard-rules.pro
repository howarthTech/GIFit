# GIFit ProGuard rules
# Keep the GIF encoder classes since they use byte-level operations
-keep class com.gifit.app.gif.** { *; }
