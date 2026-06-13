package com.gifit.app.gif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.gifit.app.model.QuantizerType
import java.io.BufferedOutputStream
import java.io.OutputStream

/**
 * Encodes a list of Bitmaps into an animated GIF89a stream.
 * Supports per-frame delays, per-frame text overlays, and multiple quantization algorithms.
 */
class AnimatedGifEncoder {

    fun encode(
        frames: List<Bitmap>,
        perFrameDelays: List<Int>,
        outputStream: OutputStream,
        perFrameOverlays: List<String?> = emptyList(),
        quantizerType: QuantizerType = QuantizerType.MEDIAN_CUT,
        onProgress: ((currentFrame: Int, totalFrames: Int) -> Unit)? = null
    ) {
        require(frames.size >= 2) { "At least 2 frames required" }

        // Frames may have differing dimensions (mixed aspect ratios, rotation, crop).
        // The GIF has a single logical screen, so normalize every frame onto a common
        // canvas large enough to hold the biggest one — centered, letterboxed on black.
        val width = frames.maxOf { it.width }
        val height = frames.maxOf { it.height }

        // Extract ARGB pixels from all frames, normalizing size and applying overlay.
        val allArgbFrames = frames.mapIndexed { index, frame ->
            val normalized = normalizeToCanvas(frame, width, height)
            val overlayText = perFrameOverlays.getOrNull(index)
            val bitmap = if (!overlayText.isNullOrBlank()) {
                drawTextOverlay(normalized, overlayText)
            } else {
                normalized
            }
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            // Recycle any intermediate bitmaps we created; never the caller's originals.
            if (bitmap !== normalized) bitmap.recycle()
            if (normalized !== frame) normalized.recycle()
            pixels
        }

        // Build global palette using selected quantizer
        val quantizer: Quantizer = when (quantizerType) {
            QuantizerType.MEDIAN_CUT -> MedianCutQuantizer()
            QuantizerType.NEUQUANT -> NeuQuantQuantizerAdapter()
        }
        val palette = quantizer.buildPalette(allArgbFrames)

        // Buffer output: GifWriter/LzwEncoder emit many tiny writes, which are a
        // syscall each on an unbuffered FileOutputStream.
        val out = if (outputStream is BufferedOutputStream) outputStream
        else BufferedOutputStream(outputStream, 16 * 1024)

        // Write GIF structure
        GifWriter.writeHeader(out)
        GifWriter.writeLogicalScreenDescriptor(out, width, height)
        GifWriter.writeColorTable(out, palette)
        GifWriter.writeNetscapeExtension(out, loops = 0)

        // Encode each frame
        for (i in allArgbFrames.indices) {
            val indexedPixels = quantizer.mapPixels(allArgbFrames[i])
            val delayCs = toDelayCentiseconds(perFrameDelays.getOrElse(i) { 50 })

            GifWriter.writeGraphicControlExtension(out, delayCs)
            GifWriter.writeImageDescriptor(out, width, height)

            val lzw = LzwEncoder(indexedPixels, 8)
            lzw.encode(out)

            onProgress?.invoke(i + 1, allArgbFrames.size)
        }

        GifWriter.writeTrailer(out)
        out.flush()
    }

    /**
     * Convert a per-frame delay in milliseconds to GIF centiseconds, rounding to the
     * nearest unit and clamping to a minimum of 2cs. Most browsers/viewers silently
     * treat delays below ~2cs as 10cs, so flooring sub-20ms delays to 0 produced
     * inconsistent playback speed.
     */
    private fun toDelayCentiseconds(delayMs: Int): Int {
        val cs = (delayMs + 5) / 10
        return cs.coerceAtLeast(2)
    }

    /**
     * Return [source] unchanged if it already fills the canvas, otherwise draw it
     * centered on an opaque black [canvasWidth] x [canvasHeight] bitmap.
     */
    private fun normalizeToCanvas(source: Bitmap, canvasWidth: Int, canvasHeight: Int): Bitmap {
        if (source.width == canvasWidth && source.height == canvasHeight) return source
        val canvasBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.BLACK)
        val left = (canvasWidth - source.width) / 2f
        val top = (canvasHeight - source.height) / 2f
        canvas.drawBitmap(source, left, top, null)
        return canvasBitmap
    }

    private fun drawTextOverlay(source: Bitmap, text: String): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val width = copy.width.toFloat()
        val height = copy.height.toFloat()

        val textSize = width / 12f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            strokeWidth = textSize / 8f
            style = Paint.Style.STROKE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val x = width / 2f
        val y = height - textSize

        canvas.drawText(text, x, y, shadowPaint)
        canvas.drawText(text, x, y, textPaint)

        return copy
    }
}
