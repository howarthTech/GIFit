package com.gifit.app.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gifit.app.gif.GifEstimator
import com.gifit.app.model.GifSettings
import com.gifit.app.model.PhotoFrame
import com.gifit.app.ui.components.CropDialog
import com.gifit.app.ui.components.FrameEditDialog
import com.gifit.app.ui.components.IntervalSlider
import com.gifit.app.ui.components.PhotoItem
import com.gifit.app.ui.components.QualitySelector
import com.gifit.app.ui.components.ResolutionSelector
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPreview: (frames: List<PhotoFrame>, settings: GifSettings) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val frames by viewModel.frames.collectAsStateWithLifecycle()
    val gifSettings by viewModel.gifSettings.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    var editingFrameId by remember { mutableStateOf<String?>(null) }
    var croppingFrameId by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
            }
            viewModel.addPhotos(uris)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.reorderPhotos(from.index, to.index)
    }

    // Frame edit bottom sheet
    editingFrameId?.let { frameId ->
        val frame = frames.find { it.id == frameId }
        val frameIndex = frames.indexOfFirst { it.id == frameId }
        if (frame != null) {
            FrameEditDialog(
                frameIndex = frameIndex,
                currentDelayMs = frame.delayMs,
                globalDelayMs = gifSettings.globalDelayMs,
                currentOverlayText = frame.overlayText,
                globalOverlayText = gifSettings.globalOverlayText,
                onDismiss = { editingFrameId = null },
                onDelayChanged = { viewModel.setPerFrameDelay(frameId, it) },
                onOverlayChanged = { viewModel.setPerFrameOverlay(frameId, it) }
            )
        }
    }

    // Crop bottom sheet
    croppingFrameId?.let { frameId ->
        val frame = frames.find { it.id == frameId }
        if (frame != null) {
            CropDialog(
                uri = frame.uri,
                currentCrop = frame.cropRect,
                onDismiss = { croppingFrameId = null },
                onCropApplied = { rect ->
                    viewModel.setCropRect(frameId, rect)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GIFit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo"
                        )
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo"
                        )
                    }
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photos"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (frames.size >= 2) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onNavigateToPreview(frames, gifSettings)
                    },
                    icon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    },
                    text = { Text("Preview GIF") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (frames.isEmpty()) {
                // Improved empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Create your GIF",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add at least 2 photos to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Add Photos")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(frames, key = { _, frame -> frame.id }) { index, frame ->
                        ReorderableItem(reorderableLazyListState, key = frame.id) { isDragging ->
                            LaunchedEffect(isDragging) {
                                if (isDragging) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            PhotoItem(
                                index = index,
                                uri = frame.uri,
                                rotationDegrees = frame.rotationDegrees,
                                flipHorizontal = frame.flipHorizontal,
                                flipVertical = frame.flipVertical,
                                hasCustomDelay = frame.delayMs != null,
                                hasCustomOverlay = frame.overlayText != null,
                                onRotate = { viewModel.rotatePhoto(frame.id) },
                                onFlipHorizontal = { viewModel.flipPhotoHorizontal(frame.id) },
                                onFlipVertical = { viewModel.flipPhotoVertical(frame.id) },
                                onDelete = { viewModel.removePhoto(frame.id) },
                                onDuplicate = { viewModel.duplicateFrame(frame.id) },
                                onEdit = { editingFrameId = frame.id },
                                dragModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                }

                // Settings section
                Spacer(modifier = Modifier.height(4.dp))

                ResolutionSelector(
                    selected = gifSettings.resolutionPreset,
                    onSelect = { viewModel.setResolution(it) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                QualitySelector(
                    selected = gifSettings.quantizerType,
                    onSelect = { viewModel.setQuantizerType(it) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Text overlay input
                OutlinedTextField(
                    value = gifSettings.globalOverlayText,
                    onValueChange = { viewModel.setOverlayText(it) },
                    label = { Text("Global text overlay (optional)") },
                    placeholder = { Text("Enter text to display on all frames") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                IntervalSlider(
                    intervalMs = gifSettings.globalDelayMs,
                    onIntervalChange = { viewModel.setInterval(it) }
                )

                // Estimated file size
                if (frames.size >= 2) {
                    val estimate = GifEstimator.estimateReadable(
                        frameCount = frames.size,
                        resolutionPreset = gifSettings.resolutionPreset,
                        quantizerType = gifSettings.quantizerType
                    )
                    Text(
                        text = "Estimated size: $estimate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(80.dp)) // Room for FAB
            }
        }
    }
}
