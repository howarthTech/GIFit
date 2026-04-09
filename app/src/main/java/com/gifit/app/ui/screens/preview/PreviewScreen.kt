package com.gifit.app.ui.screens.preview

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import com.gifit.app.gif.GifEstimator
import com.gifit.app.model.GifSettings
import com.gifit.app.model.OverlayFont
import com.gifit.app.model.OverlayTextColor
import com.gifit.app.model.PhotoFrame
import com.gifit.app.ui.components.AnimatedGifPreview
import com.gifit.app.util.MediaStoreSaver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    photoFrames: List<PhotoFrame>,
    gifSettings: GifSettings,
    onNavigateBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val frames by viewModel.frames.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val gifBytes by viewModel.gifBytes.collectAsStateWithLifecycle()
    val savedUri by viewModel.savedUri.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val intervalMs = gifSettings.globalDelayMs
    val overlayText = gifSettings.globalOverlayText
    val overlayTextX by viewModel.overlayTextX.collectAsStateWithLifecycle()
    val overlayTextY by viewModel.overlayTextY.collectAsStateWithLifecycle()
    val overlayTextScale by viewModel.overlayTextScale.collectAsStateWithLifecycle()
    val overlayTextRotation by viewModel.overlayTextRotation.collectAsStateWithLifecycle()
    val overlayTextColor by viewModel.overlayTextColor.collectAsStateWithLifecycle()
    val overlayTextBackground by viewModel.overlayTextBackground.collectAsStateWithLifecycle()
    val overlayTextFont by viewModel.overlayTextFont.collectAsStateWithLifecycle()

    // When true, share the GIF as soon as bytes are ready
    var shareOnceReady by remember { mutableStateOf(false) }

    // Build per-frame delays for preview
    val perFrameDelays = remember(photoFrames, intervalMs) {
        photoFrames.map { it.delayMs ?: intervalMs }
    }

    // Build per-frame overlays for preview
    val perFrameOverlays = remember(photoFrames, overlayText) {
        photoFrames.map { it.overlayText ?: overlayText.ifBlank { null } }
    }

    val hasOverlay = perFrameOverlays.any { !it.isNullOrBlank() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.saveGif(context)
    }

    fun shareGif(bytes: ByteArray) {
        val shareUri = MediaStoreSaver.shareTempGif(context, bytes)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/gif"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share GIF"))
    }

    LaunchedEffect(photoFrames) {
        if (frames.isEmpty()) {
            viewModel.loadFrames(context, photoFrames, gifSettings.resolutionPreset.maxWidth, gifSettings)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(savedUri) {
        savedUri?.let { snackbarHostState.showSnackbar("GIF saved to gallery!") }
    }

    // Fire share intent as soon as bytes arrive if the user requested it before generation
    LaunchedEffect(gifBytes, shareOnceReady) {
        if (shareOnceReady && gifBytes != null) {
            shareGif(gifBytes!!)
            shareOnceReady = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading frames...")
                    }
                }
            } else if (frames.isNotEmpty()) {
                AnimatedGifPreview(
                    frames = frames,
                    delayMs = intervalMs,
                    perFrameDelays = perFrameDelays,
                    perFrameOverlays = perFrameOverlays,
                    overlayTextX = overlayTextX,
                    overlayTextY = overlayTextY,
                    overlayTextScale = overlayTextScale,
                    overlayTextRotation = overlayTextRotation,
                    overlayTextColor = overlayTextColor,
                    overlayTextBackground = overlayTextBackground,
                    overlayTextFont = overlayTextFont,
                    onOverlayChange = { x, y, scale, rotation ->
                        viewModel.updateOverlayTransform(x, y, scale, rotation)
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val seconds = "%.1f".format(intervalMs / 1000f)
                Text(
                    text = "${frames.size} frames  •  ${seconds}s default interval",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasOverlay) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Gesture hint + reset
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Drag  •  Pinch  •  Twist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.resetOverlayTransform() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Color picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlayTextColor.entries.forEach { colorOption ->
                            val isSelected = colorOption == overlayTextColor
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(colorOption.colorLong), CircleShape)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                                    )
                                    .clickable { viewModel.updateOverlayColor(colorOption) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Font selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        OverlayFont.entries.forEach { fontOption ->
                            FilterChip(
                                selected = fontOption == overlayTextFont,
                                onClick = { viewModel.updateOverlayFont(fontOption) },
                                label = { Text(fontOption.label) }
                            )
                        }

                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Size info
                if (gifBytes != null) {
                    val sizeKb = gifBytes!!.size / 1024
                    Text(
                        text = "GIF ready  •  ${sizeKb}KB",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val estimate = GifEstimator.estimateReadable(
                        frameCount = frames.size,
                        resolutionPreset = gifSettings.resolutionPreset,
                        quantizerType = gifSettings.quantizerType
                    )
                    Text(
                        text = "Estimated size: $estimate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                if (gifBytes == null) {
                    // Pre-generation: Generate + Share (generates then shares automatically)
                    if (isGenerating) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val currentFrame = (progress * frames.size).toInt()
                            Text(
                                "Encoding frame $currentFrame of ${frames.size}...",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.generateGif(photoFrames, gifSettings)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Generate GIF")
                            }
                            OutlinedButton(
                                onClick = {
                                    shareOnceReady = true
                                    viewModel.generateGif(photoFrames, gifSettings)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }
                } else {
                    // Post-generation: Save + Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (savedUri == null) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    } else {
                                        viewModel.saveGif(context)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        } else {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Saved!")
                            }
                        }

                        OutlinedButton(
                            onClick = { shareGif(gifBytes!!) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}
