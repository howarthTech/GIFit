package com.gifit.app.ui.components

import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropDialog(
    uri: Uri,
    currentCrop: RectF?,
    onDismiss: () -> Unit,
    onCropApplied: (RectF?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Normalized crop rect (0..1)
    var cropRect by remember {
        mutableStateOf(currentCrop ?: RectF(0.1f, 0.1f, 0.9f, 0.9f))
    }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Crop Frame", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .onSizeChanged { containerSize = it }
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Crop preview",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit
                )

                // Crop overlay
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / containerSize.width
                                val dy = dragAmount.y / containerSize.height

                                val newLeft = (cropRect.left + dx).coerceIn(0f, cropRect.right - 0.1f)
                                val newTop = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - 0.1f)
                                val newRight = (cropRect.right + dx).coerceIn(cropRect.left + 0.1f, 1f)
                                val newBottom = (cropRect.bottom + dy).coerceIn(cropRect.top + 0.1f, 1f)

                                cropRect = RectF(newLeft, newTop, newRight, newBottom)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Dim areas outside crop
                    val dimColor = Color.Black.copy(alpha = 0.5f)
                    // Top
                    drawRect(dimColor, Offset.Zero, Size(w, cropRect.top * h))
                    // Bottom
                    drawRect(dimColor, Offset(0f, cropRect.bottom * h), Size(w, h - cropRect.bottom * h))
                    // Left
                    drawRect(dimColor, Offset(0f, cropRect.top * h), Size(cropRect.left * w, (cropRect.bottom - cropRect.top) * h))
                    // Right
                    drawRect(dimColor, Offset(cropRect.right * w, cropRect.top * h), Size(w - cropRect.right * w, (cropRect.bottom - cropRect.top) * h))

                    // Crop border
                    drawRect(
                        Color.White,
                        Offset(cropRect.left * w, cropRect.top * h),
                        Size((cropRect.right - cropRect.left) * w, (cropRect.bottom - cropRect.top) * h),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        onCropApplied(null)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Crop")
                }

                Spacer(modifier = Modifier.padding(4.dp))

                Button(
                    onClick = {
                        onCropApplied(cropRect)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
