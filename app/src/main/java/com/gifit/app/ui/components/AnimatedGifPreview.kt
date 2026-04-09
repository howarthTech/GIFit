package com.gifit.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gifit.app.model.OverlayFont
import com.gifit.app.model.OverlayTextColor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AnimatedGifPreview(
    frames: List<Bitmap>,
    delayMs: Int,
    perFrameDelays: List<Int>? = null,
    perFrameOverlays: List<String?>? = null,
    overlayTextX: Float = 0.5f,
    overlayTextY: Float = 0.5f,
    overlayTextScale: Float = 1.0f,
    overlayTextRotation: Float = 0f,
    overlayTextColor: OverlayTextColor = OverlayTextColor.WHITE,
    overlayTextBackground: Boolean = false,
    overlayTextFont: OverlayFont = OverlayFont.DEFAULT_BOLD,
    onOverlayChange: ((x: Float, y: Float, scale: Float, rotation: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (frames.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(frames, delayMs, perFrameDelays) {
        index = 0
        while (true) {
            val frameDelay = perFrameDelays?.getOrNull(index) ?: delayMs
            delay(frameDelay.toLong())
            index = (index + 1) % frames.size
        }
    }

    val currentOverlay = perFrameOverlays?.getOrNull(index)
    val hasOverlay = !currentOverlay.isNullOrBlank()

    val currentX by rememberUpdatedState(overlayTextX)
    val currentY by rememberUpdatedState(overlayTextY)
    val currentScale by rememberUpdatedState(overlayTextScale)
    val currentRotation by rememberUpdatedState(overlayTextRotation)
    val currentCallback by rememberUpdatedState(onOverlayChange)

    val fontFamily = when (overlayTextFont) {
        OverlayFont.DEFAULT_BOLD -> FontFamily.Default
        OverlayFont.SERIF        -> FontFamily.Serif
        OverlayFont.MONOSPACE    -> FontFamily.Monospace
        OverlayFont.SANS_SERIF   -> FontFamily.SansSerif
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        val currentW by rememberUpdatedState(containerWidthPx)
        val currentH by rememberUpdatedState(containerHeightPx)

        Image(
            bitmap = frames[index].asImageBitmap(),
            contentDescription = "GIF Preview - Frame ${index + 1} of ${frames.size}",
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hasOverlay && onOverlayChange != null) {
                        Modifier.pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val newX = (currentX + pan.x / currentW).coerceIn(0.05f, 0.95f)
                                val newY = (currentY + pan.y / currentH).coerceIn(0.05f, 0.95f)
                                val newScale = (currentScale * zoom).coerceIn(0.3f, 5.0f)
                                val newRotation = currentRotation + rotation
                                currentCallback?.invoke(newX, newY, newScale, newRotation)
                            }
                        }
                    } else Modifier
                ),
            contentScale = ContentScale.Fit
        )

        if (hasOverlay) {
            var textSize by remember { mutableStateOf(IntSize.Zero) }

            Text(
                text = currentOverlay!!,
                color = Color(overlayTextColor.colorLong),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (36f * overlayTextScale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 8f)
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(
                        if (overlayTextBackground) Modifier
                            .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        else Modifier
                    )
                    .onSizeChanged { textSize = it }
                    .offset {
                        IntOffset(
                            x = (overlayTextX * containerWidthPx - textSize.width / 2f).roundToInt(),
                            y = (overlayTextY * containerHeightPx - textSize.height / 2f).roundToInt()
                        )
                    }
                    .graphicsLayer { rotationZ = overlayTextRotation }
            )
        }
    }
}
