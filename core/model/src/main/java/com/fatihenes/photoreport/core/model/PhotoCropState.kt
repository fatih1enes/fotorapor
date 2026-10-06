package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

@Keep
enum class CropAspectRatio(val ratioWidth: Float, val ratioHeight: Float) {
    ORIGINAL(0f, 0f),
    FREE(0f, 0f),
    SQUARE_1_1(1f, 1f),
    RATIO_4_3(4f, 3f),
    RATIO_3_4(3f, 4f),
    RATIO_16_9(16f, 9f),
    RATIO_9_16(9f, 16f);

    val hasFixedRatio: Boolean
        get() = ratioWidth > 0f && ratioHeight > 0f

    val targetAspectRatio: Float
        get() = if (hasFixedRatio) ratioWidth / ratioHeight else 1f
}

/**
 * Non-destructive crop, rotate, flip, and straighten state.
 * Normalized coordinates in [0f, 1f] range relative to original oriented bitmap.
 */
@Keep
data class PhotoCropState(
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val rotationDegrees: Int = 0,      // 0, 90, 180, 270
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val straightenAngle: Float = 0f,   // -45f .. 45f degrees
    val aspectRatioPreset: CropAspectRatio = CropAspectRatio.ORIGINAL
) {
    fun isDefault(): Boolean =
        cropLeft == 0f && cropTop == 0f && cropRight == 1f && cropBottom == 1f &&
        rotationDegrees == 0 && !isFlippedHorizontal && !isFlippedVertical &&
        straightenAngle == 0f && aspectRatioPreset == CropAspectRatio.ORIGINAL

    val cropWidth: Float
        get() = (cropRight - cropLeft).coerceAtLeast(0.01f)

    val cropHeight: Float
        get() = (cropBottom - cropTop).coerceAtLeast(0.01f)

    fun withCropBounds(left: Float, top: Float, right: Float, bottom: Float): PhotoCropState =
        copy(cropLeft = left, cropTop = top, cropRight = right, cropBottom = bottom)

    companion object {
        val DEFAULT = PhotoCropState()
    }
}
