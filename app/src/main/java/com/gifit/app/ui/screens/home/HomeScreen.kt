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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gifit.app.model.PhotoFrame
import com.gifit.app.ui.components.IntervalSlider
import com.gifit.app.ui.components.PhotoItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPreview: (frames: List<PhotoFrame>, intervalMs: Int, overlayText: String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val frames by viewModel.frames.collectAsStateWithLifecycle()
    val intervalMs by viewModel.intervalMs.collectAsStateWithLifecycle()
    val overlayText by viewModel.overlayText.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Take persistable permissions so URIs survive process death
            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some providers don't support persistable permissions
                }
            }
            viewModel.addPhotos(uris)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.reorderPhotos(from.index, to.index)
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
                        onNavigateToPreview(frames, intervalMs, overlayText)
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No photos yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add photos for your GIF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            PhotoItem(
                                index = index,
                                uri = frame.uri,
                                rotationDegrees = frame.rotationDegrees,
                                flipHorizontal = frame.flipHorizontal,
                                flipVertical = frame.flipVertical,
                                onRotate = { viewModel.rotatePhoto(frame.id) },
                                onFlipHorizontal = { viewModel.flipPhotoHorizontal(frame.id) },
                                onFlipVertical = { viewModel.flipPhotoVertical(frame.id) },
                                onDelete = { viewModel.removePhoto(frame.id) },
                                dragModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                }

                // Text overlay input
                OutlinedTextField(
                    value = overlayText,
                    onValueChange = { viewModel.setOverlayText(it) },
                    label = { Text("Text overlay (optional)") },
                    placeholder = { Text("Enter text to display on GIF") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                IntervalSlider(
                    intervalMs = intervalMs,
                    onIntervalChange = { viewModel.setInterval(it) }
                )

                Spacer(modifier = Modifier.height(80.dp)) // Room for FAB
            }
        }
    }
}
