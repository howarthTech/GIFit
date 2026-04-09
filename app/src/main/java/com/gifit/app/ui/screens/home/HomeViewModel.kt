package com.gifit.app.ui.screens.home

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.gifit.app.model.PhotoFrame
import com.gifit.app.model.PhotoFrameState
import com.gifit.app.model.toState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _frames = MutableStateFlow<List<PhotoFrame>>(
        savedStateHandle.get<List<PhotoFrameState>>("frames")
            ?.map { it.toPhotoFrame() } ?: emptyList()
    )
    val frames: StateFlow<List<PhotoFrame>> = _frames.asStateFlow()

    private val _intervalMs = MutableStateFlow(
        savedStateHandle.get<Int>("intervalMs") ?: 500
    )
    val intervalMs: StateFlow<Int> = _intervalMs.asStateFlow()

    private val _overlayText = MutableStateFlow(
        savedStateHandle.get<String>("overlayText") ?: ""
    )
    val overlayText: StateFlow<String> = _overlayText.asStateFlow()

    private fun saveFrames() {
        savedStateHandle["frames"] = _frames.value.map { it.toState() }
    }

    fun addPhotos(uris: List<Uri>) {
        val newFrames = uris.map { PhotoFrame(uri = it) }
        _frames.value = _frames.value + newFrames
        saveFrames()
    }

    fun removePhoto(id: String) {
        _frames.value = _frames.value.filter { it.id != id }
        saveFrames()
    }

    fun reorderPhotos(from: Int, to: Int) {
        val list = _frames.value.toMutableList()
        list.add(to, list.removeAt(from))
        _frames.value = list
        saveFrames()
    }

    fun rotatePhoto(id: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(rotationDegrees = (frame.rotationDegrees + 90) % 360)
            } else frame
        }
        saveFrames()
    }

    fun flipPhotoHorizontal(id: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(flipHorizontal = !frame.flipHorizontal)
            } else frame
        }
        saveFrames()
    }

    fun flipPhotoVertical(id: String) {
        _frames.value = _frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(flipVertical = !frame.flipVertical)
            } else frame
        }
        saveFrames()
    }

    fun setInterval(ms: Int) {
        _intervalMs.value = ms
        savedStateHandle["intervalMs"] = ms
    }

    fun setOverlayText(text: String) {
        _overlayText.value = text
        savedStateHandle["overlayText"] = text
    }

    fun clearAll() {
        _frames.value = emptyList()
        saveFrames()
    }
}
