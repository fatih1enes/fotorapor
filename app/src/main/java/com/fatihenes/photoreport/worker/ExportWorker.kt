package com.fatihenes.photoreport.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.ForegroundInfo
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.core.export.HtmlExporter
import com.fatihenes.photoreport.core.export.PdfExportManager
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.database.ProjectEntity
import com.fatihenes.photoreport.core.database.LogWithPhotos
import com.fatihenes.photoreport.core.database.DailyLogEntity
import com.fatihenes.photoreport.core.database.PhotoEntity
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
        private const val CHANNEL_ID = "export_channel"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            PROGRESS_NOTIFICATION_ID,
            createNotification(context.getString(R.string.export_notif_title), "")
        )
    }

    private fun createNotification(title: String, content: String, current: Int = 0, total: Int = 0): android.app.Notification {
        val channel = android.app.NotificationChannel(CHANNEL_ID, "Export Service", android.app.NotificationManager.IMPORTANCE_LOW)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setLargeIcon(getAppIconBitmap())
            .setOngoing(true)
            .apply { if (total > 0) setProgress(total, current, false) }
            .build()
    }

    private fun showProgressNotification(projectName: String, current: Int = 0, total: Int = 0) {
        try {
            val contentText = if (total > 0) context.getString(R.string.export_progress_text, current, total)
            else context.getString(R.string.export_notif_text, projectName)

            val notification = createNotification(context.getString(R.string.export_notif_title), contentText, current, total)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(PROGRESS_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("ExportWorker", "Notification failed: ${e.message}")
        }
    }

    private fun cancelProgressNotification() {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(PROGRESS_NOTIFICATION_ID)
        } catch (_: Exception) { }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val projectName = inputData.getString("project_name") ?: "Proje"
        runExportSafely(projectName)
    }

    private suspend fun runExportSafely(projectName: String): Result {
        return try {
            executeExportFlow(projectName)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            handleExportError(e, projectName)
        }
    }

    private suspend fun executeExportFlow(projectName: String): Result {
        setForeground(getForegroundInfo())
        showProgressNotification(projectName)

        val data = fetchExportData() ?: return Result.failure(workDataOf("error" to "Data not found"))
        val uri = performExportAction(data.first, data.second, projectName)

        return handleExportResult(uri, projectName)
    }

    private fun handleExportResult(uri: Uri?, projectName: String): Result {
        return if (uri != null) {
            showCompletionNotification(uri, inputData.getString("format") ?: "PDF", projectName)
            Result.success()
        } else {
            Result.failure(workDataOf("error" to context.getString(R.string.error_unknown)))
        }
    }

    private suspend fun fetchExportData(): Pair<ProjectEntity, List<LogWithPhotos>>? {
        val projectId = inputData.getLong("project_id", -1)
        if (projectId == -1L) return null
        val project = repository.getProjectByIdSuspend(projectId) ?: return null
        val logs = repository.getLogsWithPhotosForProject(projectId)
        return project to logs
    }

    private suspend fun performExportAction(project: ProjectEntity, logs: List<LogWithPhotos>, projectName: String): Uri? {
        val format = inputData.getString("format") ?: "PDF"
        val quality = inputData.getInt("quality", 100)
        val language = inputData.getString("language") ?: "tr"

        return if (format == "PDF") {
            exportPdf(project, logs, quality, language, projectName)
        } else {
            exportZip(project, logs, quality, language)
        }
    }

    private suspend fun exportPdf(project: ProjectEntity, logs: List<LogWithPhotos>, quality: Int, language: String, projectName: String): Uri? {
        val res = pdfExportManager.exportToPdf(project, logs, quality, language) { cur, tot ->
            showProgressNotification(projectName, cur, tot)
        }
        return if (res is OperationResult.Success) res.data else null
    }

    private suspend fun exportZip(project: ProjectEntity, logs: List<LogWithPhotos>, quality: Int, language: String): Uri? {
        val dailyLogs: List<DailyLogEntity> = logs.map { it.log }
        val photoEntities: List<PhotoEntity> = logs.flatMap { it.photos }
        return HtmlExporter.exportToHtmlZip(context, project, dailyLogs, photoEntities, quality, language)
    }

    private fun handleExportError(e: Throwable, projectName: String): Result {
        Log.e("ExportWorker", "Export failed", e)
        val isRetryable = e is java.io.IOException || e is OutOfMemoryError || e is android.database.sqlite.SQLiteException
        if (isRetryable && runAttemptCount < 3) return Result.retry()

        val msg = when (e) {
            is OutOfMemoryError -> context.getString(R.string.error_out_of_memory)
            is java.io.IOException -> context.getString(R.string.error_storage_access)
            else -> e.localizedMessage ?: context.getString(R.string.error_unknown)
        }
        showFailureNotification(projectName, msg)
        return Result.failure(workDataOf("error" to msg))
    }

    private fun showFailureNotification(projectName: String, errorMessage: String) {
        cancelProgressNotification()
        val channelId = "export_error_channel"
        val channel = android.app.NotificationChannel(channelId, context.getString(R.string.export_error_channel_name), android.app.NotificationManager.IMPORTANCE_HIGH)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.export_failed_title))
            .setContentText(context.getString(R.string.export_failed_text, projectName, errorMessage))
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showCompletionNotification(uri: Uri, format: String, projectName: String) {
        cancelProgressNotification()
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = if (format == "PDF") "application/pdf" else "application/zip"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, context.getString(R.string.export_share_chooser)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(context, uri.hashCode(), chooser, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

        val channelId = "export_complete_channel"
        val channel = android.app.NotificationChannel(channelId, context.getString(R.string.export_complete_channel), android.app.NotificationManager.IMPORTANCE_HIGH)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.createNotificationChannel(channel)

        val formatName = if (format == "PDF") context.getString(R.string.export_format_pdf) else context.getString(R.string.export_format_zip)
        val appIcon = getAppIconBitmap()
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.export_ready_title))
            .setContentText(context.getString(R.string.export_ready_text, projectName, formatName))
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setLargeIcon(appIcon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_share, context.getString(R.string.export_share_action), pendingIntent)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)

        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        if (drawable !is android.graphics.drawable.BitmapDrawable) appIcon?.recycle()
    }

    private fun getAppIconBitmap(): android.graphics.Bitmap? {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
        if (drawable is android.graphics.drawable.BitmapDrawable) return drawable.bitmap
        val bitmap = createBitmap((drawable.intrinsicWidth.takeIf { it > 0 } ?: 108), (drawable.intrinsicHeight.takeIf { it > 0 } ?: 108), android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
