package com.gifit.app.gif

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NeuQuantQuantizerAdapterTest {

    @Test
    fun buildPalette_returns768Bytes() {
        val adapter = NeuQuantQuantizerAdapter(sampleFactor = 1)
        // Need enough pixels for NeuQuant (minimum ~1500 bytes = 500 pixels)
        val pixels = IntArray(600) { i ->
            when (i % 3) {
                0 -> 0xFFFF0000.toInt()
                1 -> 0xFF00FF00.toInt()
                else -> 0xFF0000FF.toInt()
            }
        }
        val palette = adapter.buildPalette(listOf(pixels))
        assertEquals(768, palette.size)
    }

    @Test
    fun mapPixels_returnsValidIndices() {
        val adapter = NeuQuantQuantizerAdapter(sampleFactor = 1)
        val pixels = IntArray(600) { i ->
            when (i % 3) {
                0 -> 0xFFFF0000.toInt()
                1 -> 0xFF00FF00.toInt()
                else -> 0xFF0000FF.toInt()
            }
        }
        adapter.buildPalette(listOf(pixels))
        val indices = adapter.mapPixels(pixels)
        assertEquals(pixels.size, indices.size)
        for (idx in indices) {
            assertTrue("Index $idx out of range", idx in 0..255)
        }
    }
}
