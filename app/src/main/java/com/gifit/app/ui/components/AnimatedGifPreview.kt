package com.gifit.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.gifit.app.model.TransitionType
import kotlinx.coroutines.delay

/** Approximate duration of a live transition in the preview, in ms. */
private const val TRANSITION_MS = 500

@Composable
fun AnimatedGifPreview(
    frames: List<Bitmap>,
    delayMs: Int,
    perFrameDelays: List<Int>? = null,
    transitionType: TransitionType = TransitionType.NONE,
    modifier: Modifier = Modifier
) {
    if (frames.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }
    // 0f = current frame fully shown; animates 0f->1f while transitioning to the next.
    val progress = remember { Animatable(0f) }

    LaunchedEffect(frames, delayMs, perFrameDelays, transitionType) {
        index = 0
        progress.snapTo(0f)
        while (true) {
            val frameDelay = perFrameDelays?.getOrNull(index) ?: delayMs
            delay(frameDelay.toLong())
            if (transitionType != TransitionType.NONE && frames.size > 1) {
                progress.snapTo(0f)
                progress.animateTo(1f, animationSpec = tween(TRANSITION_MS))
            }
            index = (index + 1) % frames.size
            progress.snapTo(0f)
        }
    }

    val current = frames[index].asImageBitmap()
    val next = frames[(index + 1) % frames.size].asImageBitmap()
    val t = progress.value

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (transitionType) {
            TransitionType.NONE -> {
                Image(
                    bitmap = current,
                    contentDescription = "GIF Preview - Frame ${index + 1} of ${frames.size}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            TransitionType.CROSSFADE -> {
                Image(
                    bitmap = current,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Image(
                    bitmap = next,
                    contentDescription = "GIF Preview - Frame ${index + 1} of ${frames.size}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alpha = t
                )
            }

            TransitionType.SLIDE -> {
                Image(
                    bitmap = current,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -t * size.width },
                    contentScale = ContentScale.Fit
                )
                Image(
                    bitmap = next,
                    contentDescription = "GIF Preview - Frame ${index + 1} of ${frames.size}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = (1f - t) * size.width },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
