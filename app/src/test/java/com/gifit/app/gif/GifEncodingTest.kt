package com.gifit.app.gif

import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class GifEncodingTest {
    @Test
    fun logicalScreenDescriptorUsesLittleEndianDimensions() {
        val out = ByteArrayOutputStream()
        GifWriter.writeLogicalScreenDescriptor(out, 480, 360)
        assertArrayEquals(byteArrayOf(0xE0.toByte(), 1, 0x68, 1, 0xF7.toByte(), 0, 0), out.toByteArray())
    }

    @Test
    fun colorTablePadsMissingEntriesWithBlack() {
        val out = ByteArrayOutputStream()
        GifWriter.writeColorTable(out, byteArrayOf(12, 34, 56), size = 2)
        assertArrayEquals(byteArrayOf(12, 34, 56, 0, 0, 0), out.toByteArray())
    }

    @Test
    fun graphicControlExtensionStoresDelayAndDisposal() {
        val out = ByteArrayOutputStream()
        GifWriter.writeGraphicControlExtension(out, delayCs = 300, dispose = 2)
        assertArrayEquals(byteArrayOf(0x21, 0xF9.toByte(), 4, 8, 0x2C, 1, 0, 0), out.toByteArray())
    }

    @Test
    fun emptyLzwStreamContainsClearAndEndCodes() {
        val out = ByteArrayOutputStream()
        LzwEncoder(intArrayOf(), colorDepth = 1).encode(out)
        // Minimum code size 2; one data byte containing 3-bit clear=4 and end=5.
        assertArrayEquals(byteArrayOf(2, 1, 0x2C, 0), out.toByteArray())
    }

    @Test
    fun encodedPixelsRoundTripThroughIndependentGifDecoder() {
        // Enough varied pixels to exercise code-width growth and GIF sub-blocks.
        val width = 64
        val height = 64
        val pixels = IntArray(width * height) { i -> (i * 17 + i / 13) % 256 }
        val palette = ByteArray(256 * 3) { (it / 3).toByte() }
        val out = ByteArrayOutputStream()
        GifWriter.writeHeader(out)
        GifWriter.writeLogicalScreenDescriptor(out, width, height)
        GifWriter.writeColorTable(out, palette)
        GifWriter.writeImageDescriptor(out, width, height)
        LzwEncoder(pixels, colorDepth = 8).encode(out)
        GifWriter.writeTrailer(out)

        val image = ImageIO.read(out.toByteArray().inputStream())
        assertNotNull(image)
        assertEquals(width, image.width)
        assertEquals(height, image.height)
        for (i in pixels.indices) {
            val gray = pixels[i]
            val expected = (gray shl 16) or (gray shl 8) or gray
            assertEquals("Pixel $i", expected, image.getRGB(i % width, i / width) and 0xFFFFFF)
        }
    }
}
