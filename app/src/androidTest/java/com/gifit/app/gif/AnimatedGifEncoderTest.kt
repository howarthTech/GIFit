package com.gifit.app.gif

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gifit.app.model.QuantizerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class AnimatedGifEncoderTest {

    private fun createSolidBitmap(color: Int, width: Int = 10, height: Int = 10): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }

    @Test
    fun encode_producesValidGif89aHeader() {
        val frames = listOf(
            createSolidBitmap(Color.RED),
            createSolidBitmap(Color.GREEN),
            createSolidBitmap(Color.BLUE)
        )
        val output = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder()
        encoder.encode(
            frames = frames,
            perFrameDelays = listOf(100, 100, 100),
            outputStream = output
        )
        val bytes = output.toByteArray()
        assertTrue("GIF too small", bytes.size > 10)

        // Check GIF89a header
        val header = String(bytes.sliceArray(0..5), Charsets.US_ASCII)
        assertEquals("GIF89a", header)

        // Check trailer byte
        assertEquals(0x3B.toByte(), bytes.last())

        frames.forEach { it.recycle() }
    }

    @Test
    fun encode_withPerFrameDelays_succeeds() {
        val frames = listOf(
            createSolidBitmap(Color.RED),
            createSolidBitmap(Color.GREEN)
        )
        val output = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder()
        encoder.encode(
            frames = frames,
            perFrameDelays = listOf(500, 1000),
            outputStream = output,
            quantizerType = QuantizerType.MEDIAN_CUT
        )
        val bytes = output.toByteArray()
        assertTrue("GIF should have data", bytes.size > 100)

        frames.forEach { it.recycle() }
    }

    @Test
    fun encode_withNeuQuant_succeeds() {
        val frames = listOf(
            createSolidBitmap(Color.RED),
            createSolidBitmap(Color.BLUE)
        )
        val output = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder()
        encoder.encode(
            frames = frames,
            perFrameDelays = listOf(100, 100),
            outputStream = output,
            quantizerType = QuantizerType.NEUQUANT
        )
        val bytes = output.toByteArray()
        val header = String(bytes.sliceArray(0..5), Charsets.US_ASCII)
        assertEquals("GIF89a", header)

        frames.forEach { it.recycle() }
    }

    @Test
    fun encode_withMixedDimensions_succeeds() {
        // Frames with differing aspect ratios + a portrait frame (as a rotated photo
        // would produce). Previously this crashed because the encoder used frame[0]'s
        // dimensions for every frame's getPixels call.
        val frames = listOf(
            createSolidBitmap(Color.RED, width = 40, height = 30),
            createSolidBitmap(Color.GREEN, width = 20, height = 60),
            createSolidBitmap(Color.BLUE, width = 50, height = 50)
        )
        val output = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder()
        encoder.encode(
            frames = frames,
            perFrameDelays = listOf(100, 100, 100),
            outputStream = output
        )
        val bytes = output.toByteArray()

        val header = String(bytes.sliceArray(0..5), Charsets.US_ASCII)
        assertEquals("GIF89a", header)
        assertEquals(0x3B.toByte(), bytes.last())

        // Logical screen descriptor (bytes 6-7 width, 8-9 height) should be the max
        // dimensions across all frames: 50 x 60.
        val screenWidth = (bytes[6].toInt() and 0xFF) or ((bytes[7].toInt() and 0xFF) shl 8)
        val screenHeight = (bytes[8].toInt() and 0xFF) or ((bytes[9].toInt() and 0xFF) shl 8)
        assertEquals(50, screenWidth)
        assertEquals(60, screenHeight)

        frames.forEach { it.recycle() }
    }

    @Test
    fun encode_reportsProgress() {
        val frames = listOf(
            createSolidBitmap(Color.RED),
            createSolidBitmap(Color.GREEN),
            createSolidBitmap(Color.BLUE)
        )
        val output = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder()
        val progressUpdates = mutableListOf<Pair<Int, Int>>()

        encoder.encode(
            frames = frames,
            perFrameDelays = listOf(100, 100, 100),
            outputStream = output,
            onProgress = { current, total ->
                progressUpdates.add(current to total)
            }
        )

        assertEquals(3, progressUpdates.size)
        assertEquals(1 to 3, progressUpdates[0])
        assertEquals(2 to 3, progressUpdates[1])
        assertEquals(3 to 3, progressUpdates[2])

        frames.forEach { it.recycle() }
    }
}
