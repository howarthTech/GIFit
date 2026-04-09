package com.gifit.app.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun PhotoItem(
    index: Int,
    uri: Uri,
    rotationDegrees: Int = 0,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
    hasCustomDelay: Boolean = false,
    hasCustomOverlay: Boolean = false,
    onRotate: () -> Unit = {},
    onFlipHorizontal: () -> Unit = {},
    onFlipVertical: () -> Unit = {},
    onDelete: () -> Unit,
    onDuplicate: () -> Unit = {},
    onEdit: () -> Unit = {},
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = dragModifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AsyncImage(
                model = uri,
                contentDescription = "Photo ${index + 1}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .graphicsLayer {
                        rotationZ = rotationDegrees.toFloat()
                        scaleX = if (flipHorizontal) -1f else 1f
                        scaleY = if (flipVertical) -1f else 1f
                    },
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Frame ${index + 1}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (hasCustomDelay || hasCustomOverlay) {
                        Text(
                            text = " *",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = onRotate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate 90°",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onFlipHorizontal,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip horizontal",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onFlipVertical,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip vertical",
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = 90f },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate frame",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit frame",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove photo",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
