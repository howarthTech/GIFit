package com.gifit.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay

@Composable
fun AnimatedGifPreview(
    frames: List<Bitmap>,
    delayMs: Int,
    perFrameDelays: List<Int>? = null,
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

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = frames[index].asImageBitmap(),
            contentDescription = "GIF Preview - Frame ${index + 1} of ${frames.size}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
