package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.LogRepository
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLogsWithPhotosUseCase @Inject constructor(
    private val repository: LogRepository
) {
    operator fun invoke(projectId: Long): Flow<List<DailyLogWithPhotos>> = repository.getLogsWithPhotosForProjectFlow(projectId)
}

class GetLogForDateUseCase @Inject constructor(
    private val repository: LogRepository
) {
    suspend operator fun invoke(projectId: Long, date: Long): DailyLog? = repository.getLogForDate(projectId, date)
}

class CreateDailyLogUseCase @Inject constructor(
    private val repository: LogRepository
) {
    suspend operator fun invoke(projectId: Long, date: Long, note: String): Long {
        return repository.insertLog(projectId, date, note)
    }
}

class UpdateLogNoteUseCase @Inject constructor(
    private val repository: LogRepository
) {
    suspend operator fun invoke(id: Long, note: String) = repository.updateNote(id, note)
}
