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
    val rotationDegrees: Float = 0f
) : Parcelable {
    companion object {
        const val MIN_SIZE_FRACTION = 0.04f
        const val MAX_SIZE_FRACTION = 0.40f
    }
}
