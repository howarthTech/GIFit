package com.gifit.app.gif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.gifit.app.model.OverlayFont
import com.gifit.app.model.OverlayTextColor
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
        overlayTextX: Float = 0.5f,
        overlayTextY: Float = 0.5f,
        overlayTextScale: Float = 1.0f,
        overlayTextRotation: Float = 0f,
        overlayTextColor: OverlayTextColor = OverlayTextColor.WHITE,
        overlayTextBackground: Boolean = false,
        overlayTextFont: OverlayFont = OverlayFont.DEFAULT_BOLD,
        onProgress: ((currentFrame: Int, totalFrames: Int) -> Unit)? = null
    ) {
        require(frames.size >= 2) { "At least 2 frames required" }

        val width = frames[0].width
        val height = frames[0].height

        val allArgbFrames = frames.mapIndexed { index, frame ->
            val overlayText = perFrameOverlays.getOrNull(index)
            val bitmap = if (!overlayText.isNullOrBlank()) {
                drawTextOverlay(
                    frame, overlayText,
                    overlayTextX, overlayTextY,
                    overlayTextScale, overlayTextRotation,
                    overlayTextColor, overlayTextBackground, overlayTextFont
                )
            } else {
                frame
            }
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            if (bitmap !== frame) bitmap.recycle()
            pixels
        }

        val quantizer: Quantizer = when (quantizerType) {
            QuantizerType.MEDIAN_CUT -> MedianCutQuantizer()
            QuantizerType.NEUQUANT -> NeuQuantQuantizerAdapter()
        }
        val palette = quantizer.buildPalette(allArgbFrames)

        GifWriter.writeHeader(outputStream)
        GifWriter.writeLogicalScreenDescriptor(outputStream, width, height)
        GifWriter.writeColorTable(outputStream, palette)
        GifWriter.writeNetscapeExtension(outputStream, loops = 0)

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

    private fun drawTextOverlay(
        source: Bitmap,
        text: String,
        normalizedX: Float,
        normalizedY: Float,
        scale: Float,
        rotation: Float,
        textColor: OverlayTextColor,
        hasBackground: Boolean,
        font: OverlayFont
    ): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val width = copy.width.toFloat()
        val height = copy.height.toFloat()

        val textSize = width / 12f * scale

        val typeface = when (font) {
            OverlayFont.DEFAULT_BOLD -> Typeface.DEFAULT_BOLD
            OverlayFont.SERIF        -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            OverlayFont.MONOSPACE    -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            OverlayFont.SANS_SERIF   -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.colorLong.toInt()
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            strokeWidth = textSize / 8f
            style = Paint.Style.STROKE
        }

        val x = normalizedX * width
        val y = normalizedY * height

        canvas.save()
        canvas.rotate(rotation, x, y)

        if (hasBackground) {
            val metrics = textPaint.fontMetrics
            val textWidth = textPaint.measureText(text)
            val pad = textSize * 0.3f
            val cornerRadius = textSize * 0.2f
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(170, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(
                    x - textWidth / 2f - pad,
                    y + metrics.top - pad,
                    x + textWidth / 2f + pad,
                    y + metrics.bottom + pad
                ),
                cornerRadius, cornerRadius,
                bgPaint
            )
        }

        canvas.drawText(text, x, y, shadowPaint)
        canvas.drawText(text, x, y, textPaint)
        canvas.restore()

        return copy
    }
}
