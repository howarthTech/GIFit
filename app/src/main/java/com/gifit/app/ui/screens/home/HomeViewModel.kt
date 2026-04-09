package com.gifit.app.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.gifit.app.model.PhotoFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    private val _frames = MutableStateFlow<List<PhotoFrame>>(emptyList())
    val frames: StateFlow<List<PhotoFrame>> = _frames.asStateFlow()

    private val _intervalMs = MutableStateFlow(500)
    val intervalMs: StateFlow<Int> = _intervalMs.asStateFlow()

    private val _overlayText = MutableStateFlow("")
    val overlayText: StateFlow<String> = _overlayText.asStateFlow()

    fun addPhotos(uris: List<Uri>) {
        val newFrames = uris.map { PhotoFrame(uri = it) }
        _frames.value = _frames.value + newFrames
    }

    fun removePhoto(id: String) {
        _frames.value = _frames.value.filter { it.id != id }
    }

    fun reorderPhotos(from: Int, to: Int) {
        val list = _frames.value.toMutableList()
        list.add(to, list.removeAt(from))
        _frames.value = list
    }

    fun rotatePhoto(id: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(rotationDegrees = (frame.rotationDegrees + 90) % 360)
            } else frame
        }
    }

    fun flipPhotoHorizontal(id: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(flipHorizontal = !frame.flipHorizontal)
            } else frame
        }
    }

    fun flipPhotoVertical(id: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(flipVertical = !frame.flipVertical)
            } else frame
        }
    }

    fun setInterval(ms: Int) {
        _intervalMs.value = ms
    }

    fun setOverlayText(text: String) {
        _overlayText.value = text
    }

    fun clearAll() {
        _frames.value = emptyList()
    }
}
