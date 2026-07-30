package com.fatihenes.photoreport.worker

import androidx.core.net.toUri
import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fatihenes.photoreport.data.DailyLogDao
import com.fatihenes.photoreport.data.DailyLogEntity
import com.fatihenes.photoreport.data.PhotoDao
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.util.DateUtils
import com.fatihenes.photoreport.util.MediaProcessor
import com.fatihenes.photoreport.util.WatermarkData
import com.fatihenes.photoreport.util.WatermarkRenderer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltWorker
class PhotoProcessingWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoDao: PhotoDao,
    private val dailyLogDao: DailyLogDao,
    private val mediaProcessor: MediaProcessor,
    private val watermarkRenderer: WatermarkRenderer
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private val logCreationMutex = Mutex()
    }

    override suspend fun doWork(): Result {
        val uriString = inputData.getString("uri") ?: return Result.failure()
        val uri = uriString.toUri()
        val projectId = inputData.getLong("projectId", -1L)
        val logId = inputData.getLong("logId", -1L)
        val enableAvif = inputData.getBoolean("enableAvif", true)
        val projectName = inputData.getString("projectName") ?: ""
        
        val hasWatermark = inputData.getBoolean("hasWatermark", false)
        val watermarkData = if (hasWatermark) {
            WatermarkData(
                latitude = if (inputData.keyValueMap.containsKey("latitude")) inputData.getDouble("latitude", 0.0) else null,
                longitude = if (inputData.keyValueMap.containsKey("longitude")) inputData.getDouble("longitude", 0.0) else null,
                address = inputData.getString("address"),
                dateTime = inputData.getString("dateTime") ?: "",
                projectName = projectName
            )
        } else null

        try {
            val watermarkedUri = if (watermarkData != null) {
                watermarkRenderer.applyWatermark(appContext, uri, watermarkData)
            } else {
                uri
            }

            val finalUri = mediaProcessor.processAndOptimize(watermarkedUri, enableAvif, projectName)

            val targetLogId = if (logId != -1L) {
                logId
            } else {
                val today = DateUtils.getStartOfDayEpochMillis()
                logCreationMutex.withLock {
                    val log = dailyLogDao.getLogForDate(projectId, today)
                    log?.id ?: dailyLogDao.insertLog(
                        DailyLogEntity(projectId = projectId, date = today, note = "")
                    )
                }
            }

            photoDao.insertPhoto(PhotoEntity(logId = targetLogId, filePath = finalUri.toString()))
            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("PhotoProcessingWorker", "Background photo save failed", e)
            return Result.failure()
        }
    }
}
