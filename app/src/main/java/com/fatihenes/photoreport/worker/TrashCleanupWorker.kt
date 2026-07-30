package com.fatihenes.photoreport.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fatihenes.photoreport.repository.TrashRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trashRepository: TrashRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 30 days in milliseconds
            val threshold = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            Log.d("TrashCleanupWorker", "Starting cleanup for items deleted before $threshold")

            trashRepository.cleanOldTrash(threshold)

            Log.d("TrashCleanupWorker", "Cleanup finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("TrashCleanupWorker", "Error during trash cleanup", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
