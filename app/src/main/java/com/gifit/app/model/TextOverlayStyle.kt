package com.gifit.app.model

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
    }
}

/** Font families offered for the overlay text. */
enum class OverlayFont(val label: String) {
    SANS("Sans"),
    SERIF("Serif"),
    MONO("Mono"),
    CONDENSED("Condensed")
}
