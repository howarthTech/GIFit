package com.gifit.app.model

import android.graphics.Typeface
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Placement of the overlay text on a frame, expressed in canvas-relative terms so it
 * maps 1:1 between the on-screen preview and the baked GIF regardless of resolution.
 *
 * @param normX horizontal center, 0f (left) .. 1f (right)
 * @param normY vertical center, 0f (top) .. 1f (bottom)
 * @param sizeFraction text size as a fraction of canvas width
 * @param rotationDegrees clockwise rotation about the anchor point
 */
@Parcelize
data class TextOverlayStyle(
    val normX: Float = 0.5f,
    val normY: Float = 0.85f,
    val sizeFraction: Float = 1f / 12f,
    val rotationDegrees: Float = 0f,
    /** Packed ARGB text color. */
    val color: Int = COLOR_WHITE,
    val font: OverlayFont = OverlayFont.SANS
) : Parcelable {
    companion object {
        const val MIN_SIZE_FRACTION = 0.04f
        const val MAX_SIZE_FRACTION = 0.40f

        const val COLOR_WHITE = 0xFFFFFFFF.toInt()
        const val COLOR_BLACK = 0xFF000000.toInt()
        const val COLOR_YELLOW = 0xFFFFEB3B.toInt()
        const val COLOR_RED = 0xFFF44336.toInt()
        const val COLOR_GREEN = 0xFF4CAF50.toInt()
        const val COLOR_BLUE = 0xFF2196F3.toInt()
        const val COLOR_PINK = 0xFFFF4081.toInt()

        /** Preset swatches offered in the color picker. */
        val COLOR_SWATCHES = intArrayOf(
            COLOR_WHITE, COLOR_BLACK, COLOR_YELLOW,
            COLOR_RED, COLOR_GREEN, COLOR_BLUE, COLOR_PINK
        )

        /**
         * Perceived luminance (Rec. 601) says this color reads as dark. Shared by the
         * encoder and the live preview so the legibility outline color always matches.
         */
        fun isDarkColor(color: Int): Boolean {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            return (0.299 * r + 0.587 * g + 0.114 * b) < 110
        }
    }
}

/**
 * Font choices for the overlay text, resolved from the device's built-in system font
 * families — fully offline, nothing bundled or downloaded. A family a given OEM doesn't
 * ship falls back to the default sans-serif automatically via [Typeface.create].
 */
enum class OverlayFont(
    val label: String,
    private val family: String,
    private val typefaceStyle: Int
) {
    SANS("Sans", "sans-serif", Typeface.BOLD),
    SERIF("Serif", "serif", Typeface.BOLD),
    MONO("Mono", "monospace", Typeface.BOLD),
    CONDENSED("Condensed", "sans-serif-condensed", Typeface.BOLD),
    CASUAL("Casual", "casual", Typeface.BOLD),
    CURSIVE("Cursive", "cursive", Typeface.BOLD),
    SMALL_CAPS("Small Caps", "sans-serif-smallcaps", Typeface.BOLD),
    TYPEWRITER("Typewriter", "serif-monospace", Typeface.BOLD),
    LIGHT("Light", "sans-serif-light", Typeface.NORMAL),
    THIN("Thin", "sans-serif-thin", Typeface.NORMAL),
    MEDIUM("Medium", "sans-serif-medium", Typeface.NORMAL),
    HEAVY("Heavy", "sans-serif-black", Typeface.BOLD),
    CONDENSED_LIGHT("Condensed Light", "sans-serif-condensed-light", Typeface.NORMAL),
    ITALIC("Italic", "sans-serif", Typeface.BOLD_ITALIC),
    SERIF_ITALIC("Serif Italic", "serif", Typeface.BOLD_ITALIC),
    CONDENSED_ITALIC("Condensed Italic", "sans-serif-condensed", Typeface.BOLD_ITALIC);

    /**
     * The concrete typeface for this choice — the single source used by both the live
     * preview and the GIF encoder, so what you see is exactly what gets baked.
     */
    fun typeface(): Typeface = Typeface.create(family, typefaceStyle)
}
