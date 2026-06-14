package com.gifit.app.model

import android.graphics.RectF
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoFrameState(
    val id: String,
    val uriString: String,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val delayMs: Int? = null,
    val overlayText: String? = null,
    val cropLeft: Float? = null,
    val cropTop: Float? = null,
    val cropRight: Float? = null,
    val cropBottom: Float? = null,
    val overlayStyle: TextOverlayStyle? = null
) : Parcelable {
    fun toPhotoFrame(): PhotoFrame = PhotoFrame(
        id = id,
        uri = Uri.parse(uriString),
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        delayMs = delayMs,
        overlayText = overlayText,
        cropRect = if (cropLeft != null && cropTop != null && cropRight != null && cropBottom != null) {
            RectF(cropLeft, cropTop, cropRight, cropBottom)
        } else null,
        overlayStyle = overlayStyle
    )
}

fun PhotoFrame.toState(): PhotoFrameState = PhotoFrameState(
    id = id,
    uriString = uri.toString(),
    rotationDegrees = rotationDegrees,
    flipHorizontal = flipHorizontal,
    flipVertical = flipVertical,
    delayMs = delayMs,
    overlayText = overlayText,
    cropLeft = cropRect?.left,
    cropTop = cropRect?.top,
    cropRight = cropRect?.right,
    cropBottom = cropRect?.bottom,
    overlayStyle = overlayStyle
)
