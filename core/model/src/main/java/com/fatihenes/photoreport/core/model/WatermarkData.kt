package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Data class representing watermark information to overlay on captured photos.
 */
@Keep
data class WatermarkData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val dateTime: String = "",
    val projectName: String = ""
)
