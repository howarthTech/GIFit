package com.gifit.app.gif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.gifit.app.model.QuantizerType
import com.gifit.app.model.TextOverlayStyle
import com.gifit.app.model.TransitionType
import java.io.BufferedOutputStream
import java.io.OutputStream

/**
 * Encodes a list of Bitmaps into an animated GIF89a stream.
 * Supports per-frame delays, per-frame text overlays, and multiple quantization algorithms.
 */
class AnimatedGifEncoder {

    companion object {
        /** Delay (ms) per transition tween frame — ~25fps for smooth motion. */
        private const val TWEEN_DELAY_MS = 40
    }

    fun encode(
        frames: List<Bitmap>,
        perFrameDelays: List<Int>,
        outputStream: OutputStream,
        perFrameOverlays: List<String?> = emptyList(),
        quantizerType: QuantizerType = QuantizerType.MEDIAN_CUT,
        overlayStyle: TextOverlayStyle = TextOverlayStyle(),
        perFrameOverlayStyles: List<TextOverlayStyle?> = emptyList(),
        dither: Boolean = false,
        transitionType: TransitionType = TransitionType.NONE,
        transitionFrames: Int = 6,
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
            val style = perFrameOverlayStyles.getOrNull(index) ?: overlayStyle
            val bitmap = if (!overlayText.isNullOrBlank()) {
                drawTextOverlay(normalized, overlayText, style)
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

        // Emit one frame: quantize (optionally dithered), then write its GIF blocks.
        fun writeFrame(pixels: IntArray, delayMs: Int) {
            val indexedPixels = if (dither) {
                ditherPixels(pixels, width, height, palette, quantizer)
            } else {
                quantizer.mapPixels(pixels)
            }
            GifWriter.writeGraphicControlExtension(out, toDelayCentiseconds(delayMs))
            GifWriter.writeImageDescriptor(out, width, height)
            LzwEncoder(indexedPixels, 8).encode(out)
        }

        val tweenCount = if (transitionType == TransitionType.NONE) 0 else transitionFrames

        // Encode each real frame, inserting transition tweens between consecutive photos.
        // Tweens are generated on demand and discarded so peak memory stays one frame.
        for (i in allArgbFrames.indices) {
            writeFrame(allArgbFrames[i], perFrameDelays.getOrElse(i) { 500 })
            onProgress?.invoke(i + 1, allArgbFrames.size)

            if (tweenCount > 0 && i < allArgbFrames.lastIndex) {
                val cur = allArgbFrames[i]
                val next = allArgbFrames[i + 1]
                for (k in 1..tweenCount) {
                    val t = k.toFloat() / (tweenCount + 1)
                    writeFrame(makeTween(cur, next, t, transitionType, width, height), TWEEN_DELAY_MS)
                }
            }
        }

        GifWriter.writeTrailer(out)
        out.flush()
    }

    /** Build a single interpolated frame between [a] and [b] at progress [t] in 0..1. */
    private fun makeTween(
        a: IntArray,
        b: IntArray,
        t: Float,
        type: TransitionType,
        width: Int,
        height: Int
    ): IntArray {
        val out = IntArray(a.size)
        when (type) {
            TransitionType.CROSSFADE -> {
                val inv = 1f - t
                for (i in a.indices) {
                    val ca = a[i]; val cb = b[i]
                    val r = (((ca shr 16) and 0xFF) * inv + ((cb shr 16) and 0xFF) * t).toInt()
                    val g = (((ca shr 8) and 0xFF) * inv + ((cb shr 8) and 0xFF) * t).toInt()
                    val bl = ((ca and 0xFF) * inv + (cb and 0xFF) * t).toInt()
                    out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
                }
            }
            TransitionType.SLIDE -> {
                // b slides in from the right, pushing a off to the left.
                val shift = (t * width).toInt()
                for (y in 0 until height) {
                    val row = y * width
                    for (x in 0 until width) {
                        val src = x + shift
                        out[row + x] = if (src < width) a[row + src] else b[row + (src - width)]
                    }
                }
            }
            TransitionType.NONE -> System.arraycopy(a, 0, out, 0, a.size)
        }
        return out
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
        val typeface = style.font.typeface()
        // Keep a dark outline for legibility on any background, unless the text itself
        // is dark — then outline in white instead.
        val outlineColor = if (TextOverlayStyle.isDarkColor(style.color)) Color.WHITE else Color.BLACK

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            this.textSize = textSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            strokeWidth = textSize / 8f
            this.style = Paint.Style.STROKE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.color
            this.textSize = textSize
            this.typeface = typeface
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
