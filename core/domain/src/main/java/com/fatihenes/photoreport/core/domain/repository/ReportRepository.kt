package com.fatihenes.photoreport.core.domain.repository

import com.fatihenes.photoreport.core.model.FileSizeInfo
import com.fatihenes.photoreport.core.model.Photo

interface ReportRepository {
    suspend fun calculateFileSizes(photos: List<Photo>): FileSizeInfo
    fun enqueueExportWork(projectId: Long, projectName: String, format: String, quality: Int, language: String)
}
