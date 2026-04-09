package com.gifit.app.gif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.OutputStream

/**
 * Encodes a list of Bitmaps into an animated GIF89a stream.
 * Uses median-cut quantization and LZW compression.
 */
class AnimatedGifEncoder {

    fun encode(
        frames: List<Bitmap>,
        delayMs: Int,
        outputStream: OutputStream,
        overlayText: String? = null
    ) {
        require(frames.size >= 2) { "At least 2 frames required" }

        val width = frames[0].width
        val height = frames[0].height

        // Extract ARGB pixels from all frames, applying text overlay if needed
        val allArgbFrames = frames.map { frame ->
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

        // Build global palette using median-cut quantization
        val quantizer = MedianCutQuantizer()
        val palette = quantizer.buildPalette(allArgbFrames)

        // Delay in centiseconds (GIF spec uses 1/100th second units)
        val delayCs = delayMs / 10

        // Write GIF structure
        GifWriter.writeHeader(outputStream)
        GifWriter.writeLogicalScreenDescriptor(outputStream, width, height)
        GifWriter.writeColorTable(outputStream, palette)
        GifWriter.writeNetscapeExtension(outputStream, loops = 0)

        // Encode each frame
        for (argbPixels in allArgbFrames) {
            val indexedPixels = quantizer.mapPixels(argbPixels)

            GifWriter.writeGraphicControlExtension(outputStream, delayCs)
            GifWriter.writeImageDescriptor(outputStream, width, height)

            val lzw = LzwEncoder(indexedPixels, 8)
            lzw.encode(outputStream)
        }

        GifWriter.writeTrailer(outputStream)
        outputStream.flush()
    }

    private fun drawTextOverlay(source: Bitmap, text: String): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val width = copy.width.toFloat()
        val height = copy.height.toFloat()

        // Size text relative to image width
        val textSize = width / 12f

        // Draw shadow/outline for readability
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
        val y = height - textSize  // Near bottom

        canvas.drawText(text, x, y, shadowPaint)
        canvas.drawText(text, x, y, textPaint)

        return copy
    }
}
