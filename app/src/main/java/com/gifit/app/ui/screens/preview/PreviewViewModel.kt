package com.gifit.app.ui.screens.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gifit.app.gif.AnimatedGifEncoder
import com.gifit.app.model.PhotoFrame
import com.gifit.app.util.ImageResizer
import com.gifit.app.util.MediaStoreSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PreviewViewModel : ViewModel() {

    private val _frames = MutableStateFlow<List<Bitmap>>(emptyList())
    val frames: StateFlow<List<Bitmap>> = _frames.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _gifBytes = MutableStateFlow<ByteArray?>(null)
    val gifBytes: StateFlow<ByteArray?> = _gifBytes.asStateFlow()

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
        if (frame.rotationDegrees == 0 && !frame.flipHorizontal && !frame.flipVertical) {
            return source
        }

        val matrix = Matrix()

        if (frame.flipHorizontal) {
            matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        }
        if (frame.flipVertical) {
            matrix.postScale(1f, -1f, source.width / 2f, source.height / 2f)
        }
        if (frame.rotationDegrees != 0) {
            matrix.postRotate(frame.rotationDegrees.toFloat(), source.width / 2f, source.height / 2f)
        }

        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (result !== source) source.recycle()
        return result
    }

    fun generateGif(delayMs: Int, overlayText: String = "") {
        val currentFrames = _frames.value
        if (currentFrames.size < 2) return

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            try {
                val bytes = withContext(Dispatchers.Default) {
                    val outputStream = ByteArrayOutputStream()
                    val encoder = AnimatedGifEncoder()
                    encoder.encode(
                        currentFrames,
                        delayMs,
                        outputStream,
                        overlayText = overlayText.ifBlank { null }
                    )
                    outputStream.toByteArray()
                }
                _gifBytes.value = bytes
            } catch (e: Exception) {
                _error.value = "Failed to generate GIF: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun saveGif(context: Context) {
        val bytes = _gifBytes.value ?: return

        viewModelScope.launch {
            _error.value = null
            try {
                val uri = withContext(Dispatchers.IO) {
                    MediaStoreSaver.saveGif(context, bytes)
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
