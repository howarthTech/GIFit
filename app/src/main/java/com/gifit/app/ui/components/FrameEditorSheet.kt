package com.gifit.app.ui.components

import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gifit.app.model.OverlayFont
import com.gifit.app.model.PhotoFrame
import com.gifit.app.model.TextOverlayStyle
import kotlin.math.roundToInt

/**
 * One bottom sheet for every per-frame edit: rotate / flip / crop, delay, and the
 * overlay text with manipulable placement, color, and font. Replaces the separate
 * FrameEditDialog + CropDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameEditorSheet(
    frame: PhotoFrame,
    frameIndex: Int,
    globalDelayMs: Int,
    globalOverlayText: String,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onDelayChanged: (Int?) -> Unit,
    onOverlayTextChanged: (String?) -> Unit,
    onCropChanged: (RectF?) -> Unit,
    onStyleChanged: (TextOverlayStyle?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current

    // Local edit state, seeded once per frame id (survives rotation/flip recompositions).
    var overlayText by remember(frame.id) { mutableStateOf(frame.overlayText ?: globalOverlayText) }
    var useCustomText by remember(frame.id) { mutableStateOf(frame.overlayText != null) }
    var style by remember(frame.id) { mutableStateOf(frame.overlayStyle ?: TextOverlayStyle()) }

    val effectiveDelay = frame.delayMs ?: globalDelayMs
    var delaySeconds by remember(frame.id) { mutableFloatStateOf(effectiveDelay / 1000f) }
    var useCustomDelay by remember(frame.id) { mutableStateOf(frame.delayMs != null) }

    var cropMode by remember(frame.id) { mutableStateOf(false) }
    var cropRect by remember(frame.id) {
        mutableStateOf(frame.cropRect ?: RectF(0.1f, 0.1f, 0.9f, 0.9f))
    }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun pushStyle(newStyle: TextOverlayStyle) {
        style = newStyle
        onStyleChanged(newStyle)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Edit Frame ${frameIndex + 1}", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(12.dp))

            // ---- Preview area ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cropMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .onSizeChanged { containerSize = it }
                    ) {
                        AsyncImage(
                            model = frame.uri,
                            contentDescription = "Crop preview",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Fit
                        )
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        if (containerSize.width == 0 || containerSize.height == 0) return@detectDragGestures
                                        val dx = drag.x / containerSize.width
                                        val dy = drag.y / containerSize.height
                                        val l = (cropRect.left + dx).coerceIn(0f, cropRect.right - 0.1f)
                                        val t = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - 0.1f)
                                        val r = (cropRect.right + dx).coerceIn(cropRect.left + 0.1f, 1f)
                                        val b = (cropRect.bottom + dy).coerceIn(cropRect.top + 0.1f, 1f)
                                        cropRect = RectF(l, t, r, b)
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val dim = Color.Black.copy(alpha = 0.5f)
                            drawRect(dim, Offset.Zero, Size(w, cropRect.top * h))
                            drawRect(dim, Offset(0f, cropRect.bottom * h), Size(w, h - cropRect.bottom * h))
                            drawRect(dim, Offset(0f, cropRect.top * h), Size(cropRect.left * w, (cropRect.bottom - cropRect.top) * h))
                            drawRect(dim, Offset(cropRect.right * w, cropRect.top * h), Size(w - cropRect.right * w, (cropRect.bottom - cropRect.top) * h))
                            drawRect(
                                Color.White,
                                Offset(cropRect.left * w, cropRect.top * h),
                                Size((cropRect.right - cropRect.left) * w, (cropRect.bottom - cropRect.top) * h),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val boxWpx = with(density) { maxWidth.toPx() }
                        val boxHpx = with(density) { maxHeight.toPx() }

                        AsyncImage(
                            model = frame.uri,
                            contentDescription = "Frame ${frameIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .graphicsLayer {
                                    rotationZ = frame.rotationDegrees.toFloat()
                                    scaleX = if (frame.flipHorizontal) -1f else 1f
                                    scaleY = if (frame.flipVertical) -1f else 1f
                                },
                            contentScale = ContentScale.Fit
                        )

                        val effectiveText = if (useCustomText) overlayText else globalOverlayText
                        if (effectiveText.isNotBlank()) {
                            val fontFamily = when (style.font) {
                                OverlayFont.SERIF -> FontFamily.Serif
                                OverlayFont.MONO -> FontFamily.Monospace
                                else -> FontFamily.SansSerif
                            }
                            val fontSizeSp = with(density) { (style.sizeFraction * boxWpx).toSp() }
                            Text(
                                text = effectiveText,
                                color = Color(style.color),
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily,
                                textAlign = TextAlign.Center,
                                fontSize = fontSizeSp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        translationX = (style.normX - 0.5f) * boxWpx
                                        translationY = (style.normY - 0.5f) * boxHpx
                                        rotationZ = style.rotationDegrees
                                    }
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            // Full-area gesture surface so pinch/drag/twist works anywhere,
                            // independent of how long the text is.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(frame.id) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            pushStyle(
                                                style.copy(
                                                    normX = (style.normX + pan.x / boxWpx).coerceIn(0f, 1f),
                                                    normY = (style.normY + pan.y / boxHpx).coerceIn(0f, 1f),
                                                    sizeFraction = (style.sizeFraction * zoom).coerceIn(
                                                        TextOverlayStyle.MIN_SIZE_FRACTION,
                                                        TextOverlayStyle.MAX_SIZE_FRACTION
                                                    ),
                                                    rotationDegrees = style.rotationDegrees + rotation
                                                )
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---- Crop controls ----
            if (cropMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onCropChanged(null); cropMode = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear crop") }
                    Button(
                        onClick = { onCropChanged(cropRect); cropMode = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("Apply crop") }
                }
            } else {
                // ---- Transform row ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = onRotate,
                        label = { Text("Rotate") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.RotateRight, null, Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = onFlipHorizontal,
                        label = { Text("Flip H") },
                        leadingIcon = { Icon(Icons.Default.Flip, null, Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = onFlipVertical,
                        label = { Text("Flip V") },
                        leadingIcon = { Icon(Icons.Default.Flip, null, Modifier.size(18.dp).graphicsLayer { rotationZ = 90f }) }
                    )
                    AssistChip(
                        onClick = { cropMode = true },
                        label = { Text(if (frame.cropRect != null) "Crop ✓" else "Crop") },
                        leadingIcon = { Icon(Icons.Default.Crop, null, Modifier.size(18.dp)) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ---- Delay ----
                Text(
                    text = if (useCustomDelay) "Frame delay: ${"%.1f".format(delaySeconds)}s"
                    else "Frame delay: Default (${"%.1f".format(globalDelayMs / 1000f)}s)",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = delaySeconds,
                    onValueChange = {
                        delaySeconds = it
                        useCustomDelay = true
                        onDelayChanged((it * 1000).roundToInt())
                    },
                    valueRange = 0.1f..3.0f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth()
                )
                if (useCustomDelay) {
                    TextButton(onClick = {
                        useCustomDelay = false
                        delaySeconds = globalDelayMs / 1000f
                        onDelayChanged(null)
                    }) { Text("Reset to default") }
                }

                Spacer(Modifier.height(8.dp))

                // ---- Overlay text ----
                OutlinedTextField(
                    value = overlayText,
                    onValueChange = {
                        overlayText = it
                        useCustomText = true
                        onOverlayTextChanged(it.ifBlank { null })
                    },
                    label = { Text(if (useCustomText) "Frame text" else "Frame text (using global)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (useCustomText) {
                    TextButton(onClick = {
                        useCustomText = false
                        overlayText = globalOverlayText
                        onOverlayTextChanged(null)
                    }) { Text("Reset to global text") }
                }

                val effectiveText = if (useCustomText) overlayText else globalOverlayText
                if (effectiveText.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Drag to move • pinch to resize • twist to rotate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(8.dp))
                    Text("Text size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = style.sizeFraction,
                        onValueChange = { pushStyle(style.copy(sizeFraction = it)) },
                        valueRange = TextOverlayStyle.MIN_SIZE_FRACTION..TextOverlayStyle.MAX_SIZE_FRACTION,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (swatch in TextOverlayStyle.COLOR_SWATCHES) {
                            val selected = swatch == style.color
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { pushStyle(style.copy(color = swatch)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (font in OverlayFont.entries) {
                            FilterChip(
                                selected = font == style.font,
                                onClick = { pushStyle(style.copy(font = font)) },
                                label = { Text(font.label) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }

            Spacer(Modifier.height(16.dp))
        }
    }
}
