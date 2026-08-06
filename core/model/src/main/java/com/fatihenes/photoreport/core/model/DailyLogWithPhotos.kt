package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Core domain representation of a DailyLog combined with its Photos.
 */
@Keep
data class DailyLogWithPhotos(
    val log: DailyLog,
    val photos: List<Photo>
)
