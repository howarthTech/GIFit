package com.gifit.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GifSettings(
    val resolutionPreset: ResolutionPreset = ResolutionPreset.MEDIUM,
    val quantizerType: QuantizerType = QuantizerType.MEDIAN_CUT,
    val globalDelayMs: Int = 500,
    val globalOverlayText: String = ""
) : Parcelable

enum class QuantizerType { MEDIAN_CUT, NEUQUANT }

enum class ResolutionPreset(val maxWidth: Int, val label: String) {
    LOW(240, "240p"),
    MEDIUM(480, "480p"),
    HIGH(720, "720p"),
    FULL(1080, "1080p")
}
