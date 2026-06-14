package com.gifit.app.gif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.gifit.app.model.QuantizerType
import com.gifit.app.model.TextOverlayStyle
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
        overlayStyle: TextOverlayStyle = TextOverlayStyle(),
        dither: Boolean = false,
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
                drawTextOverlay(normalized, overlayText, overlayStyle)
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
            val indexedPixels = if (dither) {
                ditherPixels(allArgbFrames[i], width, height, palette, quantizer)
            } else {
                quantizer.mapPixels(allArgbFrames[i])
            }
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
    /**
     * Map a frame to palette indices using Floyd–Steinberg error diffusion, which
     * spreads quantization error to neighboring pixels to break up the visible banding
     * a flat nearest-color mapping leaves on gradients. Uses two sliding error rows per
     * channel to keep memory proportional to image width, not the whole frame.
     */
    private fun ditherPixels(
        argb: IntArray,
        width: Int,
        height: Int,
        palette: ByteArray,
        quantizer: Quantizer
    ): IntArray {
        val indices = IntArray(argb.size)
        var curR = FloatArray(width); var curG = FloatArray(width); var curB = FloatArray(width)
        var nxtR = FloatArray(width); var nxtG = FloatArray(width); var nxtB = FloatArray(width)

        for (y in 0 until height) {
            nxtR.fill(0f); nxtG.fill(0f); nxtB.fill(0f)
            for (x in 0 until width) {
                val p = argb[y * width + x]
                val r = ((p shr 16) and 0xFF) + curR[x]
                val g = ((p shr 8) and 0xFF) + curG[x]
                val b = (p and 0xFF) + curB[x]

                val idx = quantizer.nearestIndex(
                    r.toInt().coerceIn(0, 255),
                    g.toInt().coerceIn(0, 255),
                    b.toInt().coerceIn(0, 255)
                )
                indices[y * width + x] = idx

                val pr = palette[idx * 3].toInt() and 0xFF
                val pg = palette[idx * 3 + 1].toInt() and 0xFF
                val pb = palette[idx * 3 + 2].toInt() and 0xFF
                val er = r - pr
                val eg = g - pg
                val eb = b - pb

                // 7/16 right, 3/16 below-left, 5/16 below, 1/16 below-right
                if (x + 1 < width) {
                    curR[x + 1] += er * 7f / 16f
                    curG[x + 1] += eg * 7f / 16f
                    curB[x + 1] += eb * 7f / 16f
                    nxtR[x + 1] += er * 1f / 16f
                    nxtG[x + 1] += eg * 1f / 16f
                    nxtB[x + 1] += eb * 1f / 16f
                }
                if (x - 1 >= 0) {
                    nxtR[x - 1] += er * 3f / 16f
                    nxtG[x - 1] += eg * 3f / 16f
                    nxtB[x - 1] += eb * 3f / 16f
                }
                nxtR[x] += er * 5f / 16f
                nxtG[x] += eg * 5f / 16f
                nxtB[x] += eb * 5f / 16f
            }
            // Slide: next row becomes current; reuse buffers.
            val tR = curR; curR = nxtR; nxtR = tR
            val tG = curG; curG = nxtG; nxtG = tG
            val tB = curB; curB = nxtB; nxtB = tB
        }
        return indices
    }

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

    private fun drawTextOverlay(source: Bitmap, text: String, style: TextOverlayStyle): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val width = copy.width.toFloat()
        val height = copy.height.toFloat()

        // Size is a fraction of canvas width so it scales consistently with the preview.
        val textSize = (style.sizeFraction * width).coerceAtLeast(1f)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            strokeWidth = textSize / 8f
            this.style = Paint.Style.STROKE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        // Center the text at the normalized anchor and rotate around that point so it
        // matches the on-screen preview (which centers + rotates about the same point).
        val cx = style.normX * width
        val cy = style.normY * height
        val fm = textPaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f

        canvas.save()
        canvas.rotate(style.rotationDegrees, cx, cy)
        canvas.drawText(text, cx, baseline, shadowPaint)
        canvas.drawText(text, cx, baseline, textPaint)
        canvas.restore()

        return copy
    }
}
