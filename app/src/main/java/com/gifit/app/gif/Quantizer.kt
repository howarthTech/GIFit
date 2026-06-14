package com.gifit.app.gif

/**
 * Common interface for color quantization algorithms.
 * Reduces full-color images to a 256-color palette for GIF encoding.
 */
interface Quantizer {
    /**
     * Build a global 256-color palette from the given ARGB pixel arrays.
     * Returns a ByteArray in RGB order (3 bytes per entry, 256 entries = 768 bytes).
     */
    fun buildPalette(frames: List<IntArray>): ByteArray

    /**
     * Map an entire ARGB pixel array to palette indices.
     */
    fun mapPixels(argbPixels: IntArray): IntArray

    /**
     * Return the palette index of the color nearest to the given RGB triple.
     * Inputs may fall slightly outside 0..255 (e.g. from error diffusion) and
     * should be clamped by the implementation. Used by Floyd–Steinberg dithering.
     */
    fun nearestIndex(r: Int, g: Int, b: Int): Int
}
