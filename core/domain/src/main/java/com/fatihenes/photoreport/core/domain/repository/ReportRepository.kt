package com.fatihenes.photoreport.core.domain.repository

import com.fatihenes.photoreport.core.model.ExportState
import com.fatihenes.photoreport.core.model.FileSizeInfo
import com.fatihenes.photoreport.core.model.Photo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ReportRepository {
    suspend fun calculateFileSizes(photos: List<Photo>): FileSizeInfo
    fun enqueueExportWork(
        projectId: Long,
        projectName: String,
        format: String,
        quality: Int,
        language: String,
    ): UUID

    fun observeExportWork(workId: UUID): Flow<android.net.Uri?>
    fun observeExportState(workId: UUID): Flow<ExportState>
}
