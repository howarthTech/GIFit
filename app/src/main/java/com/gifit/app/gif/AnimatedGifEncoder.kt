package com.gifit.app.gif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.gifit.app.model.QuantizerType
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

        val width = frames[0].width
        val height = frames[0].height

        // Extract ARGB pixels from all frames, applying text overlay if needed
        val allArgbFrames = frames.mapIndexed { index, frame ->
            val overlayText = perFrameOverlays.getOrNull(index)
            val bitmap = if (!overlayText.isNullOrBlank()) {
                drawTextOverlay(frame, overlayText)
            } else {
                frame
            }
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            if (bitmap !== frame) bitmap.recycle()
            pixels
        }

        // Build global palette using selected quantizer
        val quantizer: Quantizer = when (quantizerType) {
            QuantizerType.MEDIAN_CUT -> MedianCutQuantizer()
            QuantizerType.NEUQUANT -> NeuQuantQuantizerAdapter()
        }
        val palette = quantizer.buildPalette(allArgbFrames)

        // Write GIF structure
        GifWriter.writeHeader(outputStream)
        GifWriter.writeLogicalScreenDescriptor(outputStream, width, height)
        GifWriter.writeColorTable(outputStream, palette)
        GifWriter.writeNetscapeExtension(outputStream, loops = 0)

        // Encode each frame
        for (i in allArgbFrames.indices) {
            val indexedPixels = quantizer.mapPixels(allArgbFrames[i])
            val delayCs = perFrameDelays.getOrElse(i) { 50 } / 10

            GifWriter.writeGraphicControlExtension(outputStream, delayCs)
            GifWriter.writeImageDescriptor(outputStream, width, height)

            val lzw = LzwEncoder(indexedPixels, 8)
            lzw.encode(outputStream)

            onProgress?.invoke(i + 1, allArgbFrames.size)
        }

        GifWriter.writeTrailer(outputStream)
        outputStream.flush()
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
