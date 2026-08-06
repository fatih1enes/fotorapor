package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Core domain representation of a DailyLog.
 * Pure model without Room or database framework annotations.
 */
@Keep
data class DailyLog(
    val id: Long = 0,
    val projectId: Long,
    val date: Long, // Epoch millis
    val note: String
)
