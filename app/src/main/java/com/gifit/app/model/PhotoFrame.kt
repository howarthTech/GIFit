package com.gifit.app.model

import android.graphics.RectF
import android.net.Uri
import java.util.UUID

data class PhotoFrame(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val delayMs: Int? = null,
    val overlayText: String? = null,
    val cropRect: RectF? = null,
    /** Per-frame overlay placement/color/font; null falls back to the global style. */
    val overlayStyle: TextOverlayStyle? = null
)
