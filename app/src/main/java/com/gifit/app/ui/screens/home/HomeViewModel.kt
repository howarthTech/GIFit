package com.gifit.app.ui.screens.home

import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.gifit.app.model.GifSettings
import com.gifit.app.model.PhotoFrame
import com.gifit.app.model.PhotoFrameState
import com.gifit.app.model.QuantizerType
import com.gifit.app.model.ResolutionPreset
import com.gifit.app.model.toState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
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

    private val _gifSettings = MutableStateFlow(
        savedStateHandle.get<GifSettings>("gifSettings") ?: GifSettings()
    )
    val gifSettings: StateFlow<GifSettings> = _gifSettings.asStateFlow()

    // Undo/redo history
    private val _history = mutableListOf<List<PhotoFrame>>()
    private var _historyIndex = -1

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        // Initialize history with current state
        if (_frames.value.isNotEmpty()) {
            pushHistory()
        }
    }

    private fun pushHistory() {
        // Discard any redo states
        if (_historyIndex < _history.size - 1) {
            while (_history.size > _historyIndex + 1) {
                _history.removeAt(_history.size - 1)
            }
        }
        _history.add(_frames.value.toList())
        _historyIndex = _history.size - 1
        // Cap at 20
        if (_history.size > 20) {
            _history.removeAt(0)
            _historyIndex--
        }
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        _canUndo.value = _historyIndex > 0
        _canRedo.value = _historyIndex < _history.size - 1
    }

    fun undo() {
        if (_historyIndex > 0) {
            _historyIndex--
            _frames.value = _history[_historyIndex]
            saveFrames()
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (_historyIndex < _history.size - 1) {
            _historyIndex++
            _frames.value = _history[_historyIndex]
            saveFrames()
            updateUndoRedoState()
        }
    }

    private fun updateFrames(newFrames: List<PhotoFrame>) {
        _frames.value = newFrames
        saveFrames()
        pushHistory()
    }

    private fun saveFrames() {
        savedStateHandle["frames"] = _frames.value.map { it.toState() }
    }

    private fun saveSettings() {
        savedStateHandle["gifSettings"] = _gifSettings.value
    }

    fun addPhotos(uris: List<Uri>) {
        val newFrames = uris.map { PhotoFrame(uri = it) }
        updateFrames(_frames.value + newFrames)
    }

    fun removePhoto(id: String) {
        updateFrames(_frames.value.filter { it.id != id })
    }

    fun reorderPhotos(from: Int, to: Int) {
        val list = _frames.value.toMutableList()
        list.add(to, list.removeAt(from))
        updateFrames(list)
    }

    fun rotatePhoto(id: String) {
        updateFrames(_frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(rotationDegrees = (frame.rotationDegrees + 90) % 360)
            } else frame
        })
    }

    fun flipPhotoHorizontal(id: String) {
        updateFrames(_frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(flipHorizontal = !frame.flipHorizontal)
            } else frame
        })
    }

    fun flipPhotoVertical(id: String) {
        updateFrames(_frames.value.map { frame ->
            if (frame.id == id) {
                frame.copy(flipVertical = !frame.flipVertical)
            } else frame
        })
    }

    fun duplicateFrame(id: String) {
        val index = _frames.value.indexOfFirst { it.id == id }
        if (index < 0) return
        val original = _frames.value[index]
        val duplicate = original.copy(id = UUID.randomUUID().toString())
        val list = _frames.value.toMutableList()
        list.add(index + 1, duplicate)
        updateFrames(list)
    }

    fun setPerFrameDelay(frameId: String, delayMs: Int?) {
        updateFrames(_frames.value.map { frame ->
            if (frame.id == frameId) frame.copy(delayMs = delayMs) else frame
        })
    }

    fun setPerFrameOverlay(frameId: String, text: String?) {
        updateFrames(_frames.value.map { frame ->
            if (frame.id == frameId) frame.copy(overlayText = text) else frame
        })
    }

    fun setCropRect(frameId: String, rect: RectF?) {
        updateFrames(_frames.value.map { frame ->
            if (frame.id == frameId) frame.copy(cropRect = rect) else frame
        })
    }

    fun setResolution(preset: ResolutionPreset) {
        _gifSettings.value = _gifSettings.value.copy(resolutionPreset = preset)
        saveSettings()
    }

    fun setQuantizerType(type: QuantizerType) {
        _gifSettings.value = _gifSettings.value.copy(quantizerType = type)
        saveSettings()
    }

    fun setDithering(enabled: Boolean) {
        _gifSettings.value = _gifSettings.value.copy(dithering = enabled)
        saveSettings()
    }

    fun setInterval(ms: Int) {
        _gifSettings.value = _gifSettings.value.copy(globalDelayMs = ms)
        saveSettings()
    }

    fun setOverlayText(text: String) {
        _gifSettings.value = _gifSettings.value.copy(globalOverlayText = text)
        saveSettings()
    }

    fun clearAll() {
        updateFrames(emptyList())
    }
}
