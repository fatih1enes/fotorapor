package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.ReportRepository
import com.fatihenes.photoreport.core.model.FileSizeInfo
import com.fatihenes.photoreport.core.model.Photo
import javax.inject.Inject

class CalculateFileSizesUseCase @Inject constructor(
    private val repository: ReportRepository
) {
    suspend operator fun invoke(photos: List<Photo>): FileSizeInfo = repository.calculateFileSizes(photos)
}

class ExportProjectUseCase @Inject constructor(
    private val repository: ReportRepository
) {
    operator fun invoke(projectId: Long, projectName: String, format: String, quality: Int, language: String) {
        repository.enqueueExportWork(projectId, projectName, format, quality, language)
    }
}
