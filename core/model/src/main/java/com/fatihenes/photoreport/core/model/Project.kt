package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Core domain representation of a Project.
 * Pure model without Room or database framework annotations.
 */
@Keep
data class Project(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
