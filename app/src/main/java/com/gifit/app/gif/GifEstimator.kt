package com.gifit.app.gif

import com.gifit.app.model.QuantizerType
import com.gifit.app.model.ResolutionPreset

/**
 * Estimates GIF file size without performing full encoding.
 * Uses empirical compression ratios for heuristic estimation.
 */
object GifEstimator {

    /**
     * Estimate GIF file size in bytes.
     * @param frameCount Number of frames
     * @param resolutionPreset Resolution setting
     * @param quantizerType Quantization algorithm (affects compression ratio)
     * @param aspectRatio Width/height ratio of source images (default 4:3)
     */
    fun estimateBytes(
        frameCount: Int,
        resolutionPreset: ResolutionPreset,
        quantizerType: QuantizerType = QuantizerType.MEDIAN_CUT,
        aspectRatio: Float = 4f / 3f
    ): Long {
        val width = resolutionPreset.maxWidth
        val height = (width / aspectRatio).toInt()
        val pixelsPerFrame = width.toLong() * height

        // Empirical compression ratios (indexed pixels after LZW compression)
        val compressionRatio = when (quantizerType) {
            QuantizerType.MEDIAN_CUT -> 0.40
            QuantizerType.NEUQUANT -> 0.35
        }

        val headerOverhead = 800L // GIF header, color table, extensions
        val perFrameOverhead = 20L // GCE + image descriptor per frame

        val frameDataSize = (pixelsPerFrame * compressionRatio).toLong()
        return headerOverhead + frameCount * (frameDataSize + perFrameOverhead)
    }

    /** Estimated output above this size is flagged as "large" to the user. */
    const val LARGE_THRESHOLD_BYTES = 5L * 1024 * 1024

    /**
     * A short cautionary message when the estimated GIF is large enough to be slow to
     * generate, save, or share — or null when the output is comfortably sized.
     */
    fun sizeWarning(
        frameCount: Int,
        resolutionPreset: ResolutionPreset,
        quantizerType: QuantizerType = QuantizerType.MEDIAN_CUT
    ): String? {
        val bytes = estimateBytes(frameCount, resolutionPreset, quantizerType)
        if (bytes < LARGE_THRESHOLD_BYTES) return null
        val mb = "%.1f".format(bytes / (1024.0 * 1024.0))
        return "Large GIF (~${mb}MB) — may be slow to generate and hard to share. " +
            "Try a lower resolution or fewer frames."
    }

    /**
     * Returns a human-readable size estimate string.
     */
    fun estimateReadable(
        frameCount: Int,
        resolutionPreset: ResolutionPreset,
        quantizerType: QuantizerType = QuantizerType.MEDIAN_CUT
    ): String {
        val bytes = estimateBytes(frameCount, resolutionPreset, quantizerType)
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "~${bytes / 1024}KB"
            else -> "~%.1fMB".format(bytes / (1024.0 * 1024.0))
        }
    }
}
