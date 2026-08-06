package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Core domain representation of a Photo.
 * Pure model without Room or database framework annotations.
 */
@Keep
data class Photo(
    val id: Long = 0,
    val logId: Long,
    val filePath: String,
    val rotation: Float = 0f,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
