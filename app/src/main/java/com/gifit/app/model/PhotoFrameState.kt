package com.gifit.app.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoFrameState(
    val id: String,
    val uriString: String,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
) : Parcelable {
    fun toPhotoFrame(): PhotoFrame = PhotoFrame(
        id = id,
        uri = Uri.parse(uriString),
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical
    )
}

fun PhotoFrame.toState(): PhotoFrameState = PhotoFrameState(
    id = id,
    uriString = uri.toString(),
    rotationDegrees = rotationDegrees,
    flipHorizontal = flipHorizontal,
    flipVertical = flipVertical
)
