package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Non-destructive photo adjustments state.
 * Values are normalized typically around 0.0f (neutral).
 */
@Keep
data class PhotoAdjustments(
    val brightness: Float = 0f,    // -1f .. 1f
    val contrast: Float = 0f,      // -1f .. 1f
    val exposure: Float = 0f,      // -1f .. 1f
    val saturation: Float = 0f,    // -1f .. 1f
    val temperature: Float = 0f,   // -1f (cool/blue) .. 1f (warm/amber)
    val tint: Float = 0f,          // -1f (green) .. 1f (magenta)
    val highlights: Float = 0f,    // -1f .. 1f
    val shadows: Float = 0f,       // -1f .. 1f
    val sharpness: Float = 0f,     // 0f .. 1f
    val clarity: Float = 0f,       // -1f .. 1f
    val vignette: Float = 0f,      // 0f .. 1f
    val fade: Float = 0f           // 0f .. 1f
) {
    fun isDefault(): Boolean =
        brightness == 0f && contrast == 0f && exposure == 0f &&
        saturation == 0f && temperature == 0f && tint == 0f &&
        highlights == 0f && shadows == 0f && sharpness == 0f &&
        clarity == 0f && vignette == 0f && fade == 0f

    companion object {
        val DEFAULT = PhotoAdjustments()
    }
}
