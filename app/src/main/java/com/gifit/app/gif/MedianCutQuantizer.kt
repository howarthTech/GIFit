package com.gifit.app.gif

/**
 * Median-cut color quantization algorithm.
 * Reduces 24-bit RGB images to a 256-color palette.
 */
class MedianCutQuantizer(private val maxColors: Int = 256) : Quantizer {

    private class ColorBox(
        val colors: IntArray,   // packed RGB ints
        var count: Int
    ) {
        var rMin = 255; var rMax = 0
        var gMin = 255; var gMax = 0
        var bMin = 255; var bMax = 0

        init { computeRanges() }

        private fun computeRanges() {
            rMin = 255; rMax = 0
            gMin = 255; gMax = 0
            bMin = 255; bMax = 0
            for (i in 0 until count) {
                val c = colors[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                if (r < rMin) rMin = r; if (r > rMax) rMax = r
                if (g < gMin) gMin = g; if (g > gMax) gMax = g
                if (b < bMin) bMin = b; if (b > bMax) bMax = b
            }
        }

        fun longestAxis(): Int {
            val rRange = rMax - rMin
            val gRange = gMax - gMin
            val bRange = bMax - bMin
            return when {
                rRange >= gRange && rRange >= bRange -> 0
                gRange >= rRange && gRange >= bRange -> 1
                else -> 2
            }
        }

        fun volume(): Int = (rMax - rMin + 1) * (gMax - gMin + 1) * (bMax - bMin + 1)

        fun averageColor(): Int {
            if (count == 0) return 0
            var rSum = 0L; var gSum = 0L; var bSum = 0L
            for (i in 0 until count) {
                val c = colors[i]
                rSum += (c shr 16) and 0xFF
                gSum += (c shr 8) and 0xFF
                bSum += c and 0xFF
            }
            val r = (rSum / count).toInt().coerceIn(0, 255)
            val g = (gSum / count).toInt().coerceIn(0, 255)
            val b = (bSum / count).toInt().coerceIn(0, 255)
            return (r shl 16) or (g shl 8) or b
        }
    }

    private var palette = IntArray(0)
    private val colorLookup = HashMap<Int, Int>(4096)

    /**
     * Build a palette from the given ARGB pixel arrays.
     * Returns the palette as a ByteArray in RGB order (3 bytes per entry, 256 entries).
     */
    override fun buildPalette(frames: List<IntArray>): ByteArray {
        // Collect unique colors (quantize to 15-bit for speed)
        val colorCounts = HashMap<Int, Int>(32768)
        for (argbPixels in frames) {
            for (pixel in argbPixels) {
                // Quantize to 5 bits per channel for histogram
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val key = ((r and 0xF8) shl 8) or ((g and 0xF8) shl 3) or (b shr 3)
                colorCounts[key] = (colorCounts[key] ?: 0) + 1
            }
        }

        // Convert to array for median cut
        val uniqueColors = IntArray(colorCounts.size)
        var idx = 0
        for (key in colorCounts.keys) {
            // Expand back to 8-bit
            val r = (key shr 8) and 0xF8
            val g = (key shr 3) and 0xF8
            val b = (key shl 3) and 0xF8
            uniqueColors[idx++] = (r shl 16) or (g shl 8) or b
        }

        // Median cut
        val boxes = ArrayList<ColorBox>(maxColors)
        boxes.add(ColorBox(uniqueColors, uniqueColors.size))

        while (boxes.size < maxColors) {
            // Find box with largest volume * count product
            var bestIdx = -1
            var bestScore = -1L
            for (i in boxes.indices) {
                val box = boxes[i]
                if (box.count < 2) continue
                val score = box.volume().toLong() * box.count
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = i
                }
            }
            if (bestIdx < 0) break

            val box = boxes.removeAt(bestIdx)
            val axis = box.longestAxis()

            // Sort by the longest axis (convert to List, sort, convert back)
            val sortedList = box.colors.take(box.count).sortedBy { color ->
                when (axis) {
                    0 -> (color ushr 16) and 0xFF
                    1 -> (color ushr 8) and 0xFF
                    else -> color and 0xFF
                }
            }
            val sorted = sortedList.toIntArray()

            val mid = sorted.size / 2
            val left = ColorBox(sorted.copyOfRange(0, mid), mid)
            val right = ColorBox(sorted.copyOfRange(mid, sorted.size), sorted.size - mid)
            boxes.add(left)
            boxes.add(right)
        }

        // Build palette from box averages
        palette = IntArray(maxColors)
        for (i in boxes.indices) {
            palette[i] = boxes[i].averageColor()
        }
        // Fill remaining with black
        for (i in boxes.size until maxColors) {
            palette[i] = 0
        }

        // Build lookup cache
        colorLookup.clear()

        // Return as byte array (RGB, 3 bytes per color, 256 entries)
        val result = ByteArray(maxColors * 3)
        for (i in 0 until maxColors) {
            result[i * 3] = ((palette[i] shr 16) and 0xFF).toByte()     // R
            result[i * 3 + 1] = ((palette[i] shr 8) and 0xFF).toByte()  // G
            result[i * 3 + 2] = (palette[i] and 0xFF).toByte()          // B
        }
        return result
    }

    /**
     * Map a single RGB pixel to the nearest palette index.
     */
    fun mapPixel(r: Int, g: Int, b: Int): Int {
        val key = (r shl 16) or (g shl 8) or b
        colorLookup[key]?.let { return it }

        var bestDist = Int.MAX_VALUE
        var bestIdx = 0
        for (i in palette.indices) {
            val pr = (palette[i] shr 16) and 0xFF
            val pg = (palette[i] shr 8) and 0xFF
            val pb = palette[i] and 0xFF
            val dr = r - pr
            val dg = g - pg
            val db = b - pb
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = i
                if (dist == 0) break
            }
        }

        colorLookup[key] = bestIdx
        return bestIdx
    }

    override fun nearestIndex(r: Int, g: Int, b: Int): Int =
        mapPixel(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

    /**
     * Map an entire ARGB pixel array to palette indices.
     */
    override fun mapPixels(argbPixels: IntArray): IntArray {
        val indices = IntArray(argbPixels.size)
        for (i in argbPixels.indices) {
            val pixel = argbPixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            indices[i] = mapPixel(r, g, b)
        }
        return indices
    }
}
