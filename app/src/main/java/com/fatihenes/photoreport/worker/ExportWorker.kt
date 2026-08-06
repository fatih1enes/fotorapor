package com.fatihenes.photoreport.worker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.core.export.HtmlExporter
import com.fatihenes.photoreport.core.export.PdfExportManager
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppRepository,
    private val pdfExportManager: PdfExportManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val PROGRESS_NOTIFICATION_ID = 1001
    }

    private fun showProgressNotification(projectName: String, current: Int = 0, total: Int = 0) {
        try {
            val channelId = "export_channel"
            val channel = android.app.NotificationChannel(
                channelId,
                "Export Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)

            val appIcon = getAppIconBitmap()

            val contentText = if (total > 0) {
                context.getString(R.string.export_progress_text, current, total)
            } else {
                context.getString(R.string.export_notif_text, projectName)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.export_notif_title))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_stat_logo)
                .setLargeIcon(appIcon)
                .setOngoing(true)
                .apply {
                    if (total > 0) {
                        setProgress(total, current, false)
                    }
                }
                .build()

            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("ExportWorker", "Progress notification could not be shown: ${e.message}")
        }
    }

    private fun cancelProgressNotification() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        } catch (_: Exception) { }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val projectId = inputData.getLong("project_id", -1)
            val projectName = inputData.getString("project_name") ?: "Proje"
            showProgressNotification(projectName)
            val format = inputData.getString("format") ?: return@withContext Result.failure()
            val quality = inputData.getInt("quality", 100)
            val language = inputData.getString("language") ?: "tr"

            if (projectId == -1L) return@withContext Result.failure(workDataOf("error" to "Invalid project ID"))


            val project = repository.getProjectByIdSuspend(projectId) ?: return@withContext Result.failure(workDataOf("error" to "Project not found"))
            val logsWithPhotos = repository.getLogsWithPhotosForProject(projectId)

            val uri = when (format) {
                "PDF" -> {
                    val result = pdfExportManager.exportToPdf(
                        project = project,
                        logs = logsWithPhotos,
                        quality = quality,
                        language = language
                    ) { current, total ->
                        showProgressNotification(projectName, current, total)
                    }
                    if (result is OperationResult.Success) result.data else null
                }
                "ZIP" -> {
                    val allPhotos = logsWithPhotos.flatMap { it.photos }
                    val logs = logsWithPhotos.map { it.log }
                    HtmlExporter.exportToHtmlZip(context, project, logs, allPhotos, quality, language)
                }
                else -> null
            }

            if (uri != null) {
                showCompletionNotification(uri, format, projectName)
                Result.success()
            } else {
                Result.failure(workDataOf("error" to context.getString(R.string.error_unknown)))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e("ExportWorker", "Export failed", e)
            val projectName = inputData.getString("project_name") ?: "Proje"

            val isRetryable = e is java.io.IOException || e is OutOfMemoryError || e is android.database.sqlite.SQLiteException

            if (isRetryable && runAttemptCount < 3) {
                Log.w("ExportWorker", "Retrying export due to error: ${e.message}, attempt: $runAttemptCount")
                return@withContext Result.retry()
            }

            val errorMessage = when (e) {
                is OutOfMemoryError -> context.getString(R.string.error_out_of_memory)
                is java.io.IOException -> context.getString(R.string.error_storage_access)
                else -> e.localizedMessage ?: context.getString(R.string.error_unknown)
            }

            showFailureNotification(projectName, errorMessage)
            Result.failure(workDataOf("error" to errorMessage))
        }
    }

    private fun showFailureNotification(projectName: String, errorMessage: String) {
        cancelProgressNotification()
        val channelId = "export_error_channel"
        val channel = android.app.NotificationChannel(
            channelId,
            context.getString(R.string.export_error_channel_name),
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.export_failed_title))
            .setContentText(context.getString(R.string.export_failed_text, projectName, errorMessage))
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showCompletionNotification(uri: android.net.Uri, format: String, projectName: String) {
        cancelProgressNotification()
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = if (format == "PDF") "application/pdf" else "application/zip"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, context.getString(R.string.export_share_chooser)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }



        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            uri.hashCode(),
            chooser,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "export_complete_channel"
        val channel = android.app.NotificationChannel(
            channelId,
            context.getString(R.string.export_complete_channel),
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)

        val appIcon = getAppIconBitmap()
        val formatName = if (format == "PDF") context.getString(R.string.export_format_pdf) else context.getString(R.string.export_format_zip)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.export_ready_title))
            .setContentText(context.getString(R.string.export_ready_text, projectName, formatName))
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setLargeIcon(appIcon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_share, context.getString(R.string.export_share_action), pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        // Recycle if it's a generated bitmap (Vector -> Bitmap)
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        if (drawable !is android.graphics.drawable.BitmapDrawable) {
            appIcon?.recycle()
        }
    }
    private fun getAppIconBitmap(): android.graphics.Bitmap? {
        var result: android.graphics.Bitmap? = null
        val drawable = androidx.core.content.ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher)
        if (drawable != null) {
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                result = drawable.bitmap
            } else {
                val bitmap = createBitmap(
                    (drawable.intrinsicWidth.takeIf { it > 0 } ?: 108),
                    (drawable.intrinsicHeight.takeIf { it > 0 } ?: 108),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                result = bitmap
            }
        }
        return result
    }
}
