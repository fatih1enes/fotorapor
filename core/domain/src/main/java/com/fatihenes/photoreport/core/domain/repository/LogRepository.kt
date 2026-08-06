package com.fatihenes.photoreport.core.domain.repository

import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getLogsForProject(projectId: Long): Flow<List<DailyLog>>
    suspend fun getLogForDate(projectId: Long, date: Long): DailyLog?
    suspend fun insertLog(projectId: Long, date: Long, note: String): Long
    suspend fun updateNote(id: Long, note: String)
    fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<DailyLogWithPhotos>>
    suspend fun getLogsWithPhotosForProject(projectId: Long): List<DailyLogWithPhotos>
}
