package com.gifit.app.gif

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Local JVM tests: no Android device, framework mocks, or network required. */
class GifEncodingTest {
    @Test
    fun headerAndScreenDescriptorEncodeDimensionsLittleEndian() {
        val out = ByteArrayOutputStream()
        GifWriter.writeHeader(out)
        GifWriter.writeLogicalScreenDescriptor(out, width = 320, height = 240)

        assertArrayEquals(
            bytes(0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
                0x40, 0x01, 0xF0, 0x00, 0xF7, 0x00, 0x00),
            out.toByteArray()
        )
    }

    @Test
    fun colorTablePadsShortPalettesAndTruncatesLongOnes() {
        val padded = ByteArrayOutputStream()
        GifWriter.writeColorTable(padded, bytes(255, 128, 64), size = 2)
        assertArrayEquals(bytes(255, 128, 64, 0, 0, 0), padded.toByteArray())

        val truncated = ByteArrayOutputStream()
        GifWriter.writeColorTable(truncated, bytes(1, 2, 3, 4, 5, 6, 7), size = 2)
        assertArrayEquals(bytes(1, 2, 3, 4, 5, 6), truncated.toByteArray())
    }

    @Test
    fun graphicControlExtensionPreservesDelayAndDisposal() {
        val out = ByteArrayOutputStream()
        GifWriter.writeGraphicControlExtension(out, delayCs = 300, dispose = 2)
        assertArrayEquals(bytes(0x21, 0xF9, 4, 8, 0x2C, 1, 0, 0), out.toByteArray())
    }

    @Test
    fun emptyLzwStreamContainsClearAndEndCodesAndTerminator() {
        val out = ByteArrayOutputStream()
        LzwEncoder(intArrayOf(), colorDepth = 1).encode(out)
        // Minimum depth 2; three-bit clear=4 and end=5 pack to 0b101100.
        assertArrayEquals(bytes(2, 1, 0x2C, 0), out.toByteArray())
    }

    @Test
    fun encodedGifRoundTripsThroughIndependentDecoder() {
        val width = 64
        val height = 64
        val pixels = IntArray(width * height) { index ->
            (index * 37 + index / width * 13) % 4
        }
        val palette = bytes(0, 0, 0, 255, 0, 0, 0, 255, 0, 0, 0, 255)
        val colors = intArrayOf(0x000000, 0xFF0000, 0x00FF00, 0x0000FF)
        val out = ByteArrayOutputStream()
        GifWriter.writeHeader(out)
        GifWriter.writeLogicalScreenDescriptor(out, width, height, colorTableSize = 4)
        GifWriter.writeColorTable(out, palette, size = 4)
        GifWriter.writeNetscapeExtension(out)
        GifWriter.writeGraphicControlExtension(out, delayCs = 15)
        GifWriter.writeImageDescriptor(out, width, height)
        LzwEncoder(pixels, colorDepth = 2).encode(out)
        GifWriter.writeTrailer(out)

        val image = ImageIO.read(ByteArrayInputStream(out.toByteArray()))
        assertNotNull("Java ImageIO must recognize the generated GIF", image)
        assertEquals(width, image.width)
        assertEquals(height, image.height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                assertEquals("Pixel ($x, $y)", colors[pixels[y * width + x]],
                    image.getRGB(x, y) and 0xFFFFFF)
            }
        }
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
