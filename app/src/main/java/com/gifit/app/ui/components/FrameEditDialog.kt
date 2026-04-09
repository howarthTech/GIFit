package com.gifit.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameEditDialog(
    frameIndex: Int,
    currentDelayMs: Int?,
    globalDelayMs: Int,
    currentOverlayText: String?,
    globalOverlayText: String,
    onDismiss: () -> Unit,
    onDelayChanged: (Int?) -> Unit,
    onOverlayChanged: (String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    val effectiveDelay = currentDelayMs ?: globalDelayMs
    var delaySeconds by remember { mutableFloatStateOf(effectiveDelay / 1000f) }
    var useCustomDelay by remember { mutableStateOf(currentDelayMs != null) }

    val effectiveOverlay = currentOverlayText ?: globalOverlayText
    var overlayText by remember { mutableStateOf(effectiveOverlay) }
    var useCustomOverlay by remember { mutableStateOf(currentOverlayText != null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding()
        ) {
            Text(
                text = "Edit Frame ${frameIndex + 1}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Per-frame delay
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
                valueRange = 0.1f..10.0f,
                steps = 98,
                modifier = Modifier.fillMaxWidth()
            )
            if (useCustomDelay) {
                TextButton(onClick = {
                    useCustomDelay = false
                    delaySeconds = globalDelayMs / 1000f
                    onDelayChanged(null)
                }) {
                    Text("Reset to default")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Per-frame text overlay
            OutlinedTextField(
                value = overlayText,
                onValueChange = {
                    overlayText = it
                    useCustomOverlay = true
                    onOverlayChanged(it.ifBlank { null })
                },
                label = {
                    Text(if (useCustomOverlay) "Frame text overlay" else "Frame text (using global)")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (useCustomOverlay) {
                TextButton(onClick = {
                    useCustomOverlay = false
                    overlayText = globalOverlayText
                    onOverlayChanged(null)
                }) {
                    Text("Reset to global text")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
