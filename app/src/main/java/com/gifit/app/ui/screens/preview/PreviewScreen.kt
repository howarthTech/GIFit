package com.gifit.app.ui.screens.preview

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.gifit.app.gif.GifEstimator
import com.gifit.app.model.GifSettings
import com.gifit.app.model.OverlayFont
import com.gifit.app.model.PhotoFrame
import com.gifit.app.model.TextOverlayStyle
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
    val gifFile by viewModel.gifFile.collectAsStateWithLifecycle()
    val savedUri by viewModel.savedUri.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val intervalMs = gifSettings.globalDelayMs
    var overlayText by rememberSaveable { mutableStateOf(gifSettings.globalOverlayText) }

    // Manipulable overlay placement (canvas-relative so it maps 1:1 to the baked GIF).
    var overlayX by rememberSaveable { mutableStateOf(TextOverlayStyle().normX) }
    var overlayY by rememberSaveable { mutableStateOf(TextOverlayStyle().normY) }
    var overlaySize by rememberSaveable { mutableStateOf(TextOverlayStyle().sizeFraction) }
    var overlayRotation by rememberSaveable { mutableStateOf(TextOverlayStyle().rotationDegrees) }
    var overlayColor by rememberSaveable { mutableStateOf(TextOverlayStyle().color) }
    var overlayFont by rememberSaveable { mutableStateOf(TextOverlayStyle().font) }

    val overlayFontFamily = when (overlayFont) {
        OverlayFont.SERIF -> FontFamily.Serif
        OverlayFont.MONO -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    // Build per-frame delays for preview
    val perFrameDelays = remember(photoFrames, intervalMs) {
        photoFrames.map { it.delayMs ?: intervalMs }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.saveGif(context)
    }

    LaunchedEffect(photoFrames) {
        if (frames.isEmpty()) {
            viewModel.loadFrames(context, photoFrames, gifSettings.resolutionPreset.maxWidth)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(savedUri) {
        savedUri?.let {
            snackbarHostState.showSnackbar("GIF saved to gallery!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading frames...")
                    }
                }
            } else if (frames.isNotEmpty()) {
                val density = LocalDensity.current
                // GIF canvas aspect ratio = largest frame dimensions (matches the encoder).
                val canvasAspect = remember(frames) {
                    val w = frames.maxOf { it.width }.toFloat()
                    val h = frames.maxOf { it.height }.toFloat()
                    if (h > 0f) w / h else 1f
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Fit a content box of the canvas aspect ratio inside the available area
                    // so on-screen normalized coords match the baked output exactly.
                    val maxWpx = with(density) { maxWidth.toPx() }
                    val maxHpx = with(density) { maxHeight.toPx() }
                    val contentWpx: Float
                    val contentHpx: Float
                    if (maxWpx / maxHpx > canvasAspect) {
                        contentHpx = maxHpx
                        contentWpx = maxHpx * canvasAspect
                    } else {
                        contentWpx = maxWpx
                        contentHpx = maxWpx / canvasAspect
                    }

                    Box(
                        modifier = Modifier.size(
                            with(density) { contentWpx.toDp() },
                            with(density) { contentHpx.toDp() }
                        )
                    ) {
                        val generated = gifFile
                        if (generated != null) {
                            // Show the real generated GIF — true output incl. transitions,
                            // baked text, and dithering. lastModified busts Coil's cache.
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(generated)
                                    .memoryCacheKey(generated.path + generated.lastModified())
                                    .diskCacheKey(generated.path + generated.lastModified())
                                    .build(),
                                contentDescription = "Generated GIF preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            AnimatedGifPreview(
                                frames = frames,
                                delayMs = intervalMs,
                                perFrameDelays = perFrameDelays,
                                transitionType = gifSettings.transitionType,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // WYSIWYG overlay: drag to move, pinch to resize, twist to rotate.
                        // Only while editing — once generated, text is baked into the GIF.
                        if (generated == null && overlayText.isNotBlank()) {
                            val fontSizeSp = with(density) { (overlaySize * contentWpx).toSp() }
                            Text(
                                text = overlayText,
                                color = Color(overlayColor),
                                fontWeight = FontWeight.Bold,
                                fontFamily = overlayFontFamily,
                                textAlign = TextAlign.Center,
                                fontSize = fontSizeSp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        translationX = (overlayX - 0.5f) * contentWpx
                                        translationY = (overlayY - 0.5f) * contentHpx
                                        rotationZ = overlayRotation
                                    }
                                    .pointerInput(frames) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            overlayX = (overlayX + pan.x / contentWpx).coerceIn(0f, 1f)
                                            overlayY = (overlayY + pan.y / contentHpx).coerceIn(0f, 1f)
                                            overlaySize = (overlaySize * zoom).coerceIn(
                                                TextOverlayStyle.MIN_SIZE_FRACTION,
                                                TextOverlayStyle.MAX_SIZE_FRACTION
                                            )
                                            overlayRotation += rotation
                                            if (gifFile != null) viewModel.clearGeneratedGif()
                                        }
                                    }
                                    .background(
                                        Color.Black.copy(alpha = 0.35f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (gifFile == null && overlayText.isNotBlank()) {
                    Text(
                        text = "Drag to move • pinch to resize • twist to rotate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val seconds = "%.1f".format(intervalMs / 1000f)
                Text(
                    text = "${frames.size} frames at ${seconds}s default interval",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = overlayText,
                    onValueChange = {
                        overlayText = it
                        // A generated GIF no longer matches the edited text.
                        if (gifFile != null) viewModel.clearGeneratedGif()
                    },
                    label = { Text("Overlay text (optional)") },
                    placeholder = { Text("Shown across all frames") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (overlayText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Color swatches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (swatch in TextOverlayStyle.COLOR_SWATCHES) {
                            val selected = swatch == overlayColor
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        overlayColor = swatch
                                        if (gifFile != null) viewModel.clearGeneratedGif()
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Font chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (font in OverlayFont.entries) {
                            FilterChip(
                                selected = font == overlayFont,
                                onClick = {
                                    overlayFont = font
                                    if (gifFile != null) viewModel.clearGeneratedGif()
                                },
                                label = { Text(font.label) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (gifFile == null) {
                    if (isGenerating) {
                        // Dedicated progress area, separate from the action button.
                        val currentFrame = (progress * frames.size).toInt().coerceIn(1, frames.size)
                        Text(
                            text = "Encoding frame $currentFrame of ${frames.size}...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Show estimated size
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.generateGif(
                                    context = context,
                                    photoFrames = photoFrames,
                                    gifSettings = gifSettings,
                                    globalOverlayText = overlayText,
                                    overlayStyle = TextOverlayStyle(
                                        normX = overlayX,
                                        normY = overlayY,
                                        sizeFraction = overlaySize,
                                        rotationDegrees = overlayRotation,
                                        color = overlayColor,
                                        font = overlayFont
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Generate GIF")
                        }
                    }
                } else {
                    val sizeKb = (gifFile?.length() ?: 0L) / 1024
                    Text(
                        text = "GIF ready (${sizeKb}KB)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (savedUri == null) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        permissionLauncher.launch(
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        )
                                    } else {
                                        viewModel.saveGif(context)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save to gallery")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save to Gallery")
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

                        // Share — works immediately after generation (no save required)
                        OutlinedButton(
                            onClick = {
                                val file = gifFile ?: return@OutlinedButton
                                val shareUri = MediaStoreSaver.fileProviderUri(context, file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/gif"
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share GIF")
                                )
                            },
                            enabled = gifFile != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share GIF")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}
