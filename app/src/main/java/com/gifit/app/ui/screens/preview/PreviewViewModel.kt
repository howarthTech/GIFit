package com.gifit.app.ui.screens.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gifit.app.gif.AnimatedGifEncoder
import com.gifit.app.model.GifSettings
import com.gifit.app.model.PhotoFrame
import com.gifit.app.model.QuantizerType
import com.gifit.app.model.TextOverlayStyle
import com.gifit.app.util.ImageResizer
import com.gifit.app.util.MediaStoreSaver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _frames = MutableStateFlow<List<Bitmap>>(emptyList())
    val frames: StateFlow<List<Bitmap>> = _frames.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // The encoded GIF is streamed to a cache file rather than held in memory as a
    // ByteArray. The file lives under cacheDir/shared_gifs so it can be handed
    // straight to FileProvider for sharing (see res/xml/file_paths.xml).
    private val _gifFile = MutableStateFlow<File?>(null)
    val gifFile: StateFlow<File?> = _gifFile.asStateFlow()

    private val _savedUri = MutableStateFlow<Uri?>(null)
    val savedUri: StateFlow<Uri?> = _savedUri.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadFrames(context: Context, photoFrames: List<PhotoFrame>, maxWidth: Int = 480) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val bitmaps = withContext(Dispatchers.IO) {
                    photoFrames.map { frame ->
                        val bitmap = ImageResizer.resizeBitmap(context, frame.uri, maxWidth)
                        applyTransforms(bitmap, frame)
                    }
                }
                _frames.value = bitmaps
            } catch (e: Exception) {
                _error.value = "Failed to load images: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyTransforms(source: Bitmap, frame: PhotoFrame): Bitmap {
        val needsCrop = frame.cropRect != null
        val needsTransform = frame.rotationDegrees != 0 || frame.flipHorizontal || frame.flipVertical

        if (!needsCrop && !needsTransform) return source

        var bitmap = source

        // Apply crop first (in normalized coordinates)
        if (frame.cropRect != null) {
            val rect = frame.cropRect
            val x = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val y = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val w = ((rect.right - rect.left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
            val h = ((rect.bottom - rect.top) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
            val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
            if (cropped !== bitmap) bitmap.recycle()
            bitmap = cropped
        }

        if (!needsTransform) return bitmap

        val matrix = Matrix()
        if (frame.flipHorizontal) {
            matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        if (frame.flipVertical) {
            matrix.postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        if (frame.rotationDegrees != 0) {
            matrix.postRotate(frame.rotationDegrees.toFloat(), bitmap.width / 2f, bitmap.height / 2f)
        }

        val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (result !== bitmap) bitmap.recycle()
        return result
    }

    /** Discard a previously generated GIF so the user can re-generate after edits. */
    fun clearGeneratedGif() {
        _gifFile.value = null
        _savedUri.value = null
    }

    fun generateGif(
        context: Context,
        photoFrames: List<PhotoFrame>,
        gifSettings: GifSettings,
        globalOverlayText: String = gifSettings.globalOverlayText,
        overlayStyle: TextOverlayStyle = TextOverlayStyle()
    ) {
        val currentFrames = _frames.value
        if (currentFrames.size < 2) return

        val appContext = context.applicationContext
        viewModelScope.launch {
            _isGenerating.value = true
            _progress.value = 0f
            _error.value = null
            try {
                val file = withContext(Dispatchers.Default) {
                    val cacheDir = File(appContext.cacheDir, "shared_gifs").apply { mkdirs() }
                    val outFile = File(cacheDir, "GIFit_preview.gif")
                    val encoder = AnimatedGifEncoder()

                    // Build per-frame delays
                    val perFrameDelays = photoFrames.map { frame ->
                        frame.delayMs ?: gifSettings.globalDelayMs
                    }

                    // Build per-frame overlay texts (per-frame override wins over global)
                    val perFrameOverlays = photoFrames.map { frame ->
                        frame.overlayText ?: globalOverlayText.ifBlank { null }
                    }

                    BufferedOutputStream(outFile.outputStream()).use { out ->
                        encoder.encode(
                            frames = currentFrames,
                            perFrameDelays = perFrameDelays,
                            outputStream = out,
                            perFrameOverlays = perFrameOverlays,
                            quantizerType = gifSettings.quantizerType,
                            overlayStyle = overlayStyle,
                            dither = gifSettings.dithering,
                            onProgress = { current, total ->
                                _progress.value = current.toFloat() / total
                            }
                        )
                    }
                    outFile
                }
                _gifFile.value = file
                _progress.value = 1f
            } catch (e: Exception) {
                _error.value = "Failed to generate GIF: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun saveGif(context: Context) {
        val file = _gifFile.value ?: return

        viewModelScope.launch {
            _error.value = null
            try {
                val uri = withContext(Dispatchers.IO) {
                    MediaStoreSaver.saveGif(context, file)
                }
                _savedUri.value = uri
            } catch (e: Exception) {
                _error.value = "Failed to save GIF: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        _frames.value.forEach { it.recycle() }
    }
}
