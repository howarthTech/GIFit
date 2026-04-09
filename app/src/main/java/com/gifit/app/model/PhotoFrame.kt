package com.gifit.app.model

import android.net.Uri
import java.util.UUID

data class PhotoFrame(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val rotationDegrees: Int = 0,       // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)
