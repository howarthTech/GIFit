package com.gifit.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GifSettings(
    val resolutionPreset: ResolutionPreset = ResolutionPreset.MEDIUM,
    val quantizerType: QuantizerType = QuantizerType.MEDIAN_CUT,
    val globalDelayMs: Int = 1500,
    val globalOverlayText: String = "",
    val dithering: Boolean = false,
    val transitionType: TransitionType = TransitionType.NONE
) : Parcelable

enum class QuantizerType { MEDIAN_CUT, NEUQUANT }

/** Animated transition inserted between consecutive photos. */
enum class TransitionType(val label: String) {
    NONE("None"),
    CROSSFADE("Crossfade"),
    SLIDE("Slide")
}

enum class ResolutionPreset(val maxWidth: Int, val label: String) {
    LOW(240, "240p"),
    MEDIUM(480, "480p"),
    HIGH(720, "720p"),
    FULL(1080, "1080p")
}
