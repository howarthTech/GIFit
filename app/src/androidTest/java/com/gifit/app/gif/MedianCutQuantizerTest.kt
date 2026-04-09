package com.gifit.app.gif

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedianCutQuantizerTest {

    @Test
    fun buildPalette_returns768Bytes() {
        val quantizer = MedianCutQuantizer()
        // Create a simple frame: red, green, blue pixels
        val pixels = intArrayOf(
            0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(),
            0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt()
        )
        val palette = quantizer.buildPalette(listOf(pixels))
        assertEquals(768, palette.size) // 256 colors * 3 bytes
    }

    @Test
    fun mapPixels_returnsValidIndices() {
        val quantizer = MedianCutQuantizer()
        val pixels = intArrayOf(
            0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(),
            0xFFFFFF00.toInt(), 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt()
        )
        quantizer.buildPalette(listOf(pixels))
        val indices = quantizer.mapPixels(pixels)
        assertEquals(pixels.size, indices.size)
        for (idx in indices) {
            assertTrue("Index $idx out of range", idx in 0..255)
        }
    }

    @Test
    fun mapPixels_sameColorReturnsSameIndex() {
        val quantizer = MedianCutQuantizer()
        val red = 0xFFFF0000.toInt()
        val pixels = intArrayOf(red, red, red, red)
        quantizer.buildPalette(listOf(pixels))
        val indices = quantizer.mapPixels(pixels)
        // All same color should map to same index
        assertTrue(indices.all { it == indices[0] })
    }
}
