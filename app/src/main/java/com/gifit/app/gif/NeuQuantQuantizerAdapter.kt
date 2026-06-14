package com.gifit.app.gif

/**
 * Adapter wrapping NeuQuantQuantizer to implement the Quantizer interface.
 * Converts between ARGB IntArray format and the BGR ByteArray format NeuQuant expects.
 */
class NeuQuantQuantizerAdapter(private val sampleFactor: Int = 10) : Quantizer {

    private lateinit var neuQuant: NeuQuantQuantizer

    override fun buildPalette(frames: List<IntArray>): ByteArray {
        // Convert all ARGB frames to a flat BGR byte array (NeuQuant format)
        val totalPixels = frames.sumOf { it.size }
        val bgrBytes = ByteArray(totalPixels * 3)
        var offset = 0
        for (argbPixels in frames) {
            for (pixel in argbPixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                bgrBytes[offset++] = b.toByte()
                bgrBytes[offset++] = g.toByte()
                bgrBytes[offset++] = r.toByte()
            }
        }

        neuQuant = NeuQuantQuantizer(bgrBytes, bgrBytes.size, sampleFactor)
        return neuQuant.process()
    }

    override fun mapPixels(argbPixels: IntArray): IntArray {
        val indices = IntArray(argbPixels.size)
        for (i in argbPixels.indices) {
            val pixel = argbPixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            indices[i] = neuQuant.map(b, g, r)
        }
        return indices
    }

    override fun nearestIndex(r: Int, g: Int, b: Int): Int =
        neuQuant.map(b.coerceIn(0, 255), g.coerceIn(0, 255), r.coerceIn(0, 255))
}
