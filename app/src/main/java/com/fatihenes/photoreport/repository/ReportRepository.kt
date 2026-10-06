package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.core.common.model.FileSizeInfo
import com.fatihenes.photoreport.core.database.PhotoEntity
import com.fatihenes.photoreport.core.database.mapper.toDomain
import javax.inject.Inject
import javax.inject.Singleton

fun interface ReportRepository {
    suspend fun calculateFileSizes(photos: List<PhotoEntity>): FileSizeInfo
}

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val domainReportRepository: com.fatihenes.photoreport.core.domain.repository.ReportRepository
) : ReportRepository {

    override suspend fun calculateFileSizes(photos: List<PhotoEntity>): FileSizeInfo {
        return domainReportRepository.calculateFileSizes(photos.map { it.toDomain() })
    }
}
