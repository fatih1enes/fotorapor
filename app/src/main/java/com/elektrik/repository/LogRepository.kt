package com.sarikaya.santiye.gunlugu.repository

import com.sarikaya.santiye.gunlugu.data.DailyLogDao
import com.sarikaya.santiye.gunlugu.data.DailyLogEntity
import com.sarikaya.santiye.gunlugu.data.LogWithPhotos
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface LogRepository {
    fun getLogsForProject(projectId: Long): Flow<List<DailyLogEntity>>
    suspend fun getLogForDate(projectId: Long, date: Long): DailyLogEntity?
    suspend fun insertLog(log: DailyLogEntity): Long
    suspend fun updateNote(id: Long, note: String)
    fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<LogWithPhotos>>
    suspend fun getLogsWithPhotosForProject(projectId: Long): List<LogWithPhotos>
}

@Singleton
class LogRepositoryImpl @Inject constructor(
    private val dailyLogDao: DailyLogDao
) : LogRepository {

    override fun getLogsForProject(projectId: Long): Flow<List<DailyLogEntity>> = dailyLogDao.getLogsForProject(projectId)

    override suspend fun getLogForDate(projectId: Long, date: Long): DailyLogEntity? = dailyLogDao.getLogForDate(projectId, date)

    override suspend fun insertLog(log: DailyLogEntity): Long = dailyLogDao.insertLog(log)

    override suspend fun updateNote(id: Long, note: String) = dailyLogDao.updateNote(id, note)

    override fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<LogWithPhotos>> = dailyLogDao.getLogsWithPhotosForProject(projectId)

    override suspend fun getLogsWithPhotosForProject(projectId: Long): List<LogWithPhotos> = dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)
}
