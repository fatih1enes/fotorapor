package com.fatihenes.photoreport.repository

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import com.fatihenes.photoreport.core.common.model.FileSizeInfo
import com.fatihenes.photoreport.core.database.PhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

fun interface ReportRepository {
    suspend fun calculateFileSizes(photos: List<PhotoEntity>): FileSizeInfo
}

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ReportRepository {

    override suspend fun calculateFileSizes(photos: List<PhotoEntity>): FileSizeInfo = withContext(ioDispatcher) {
        var totalPhotoBytes = 0L
        var totalVideoBytes = 0L
        var photoCount = 0
        var videoCount = 0

        var estimatedQ100 = 0L
        var estimatedQ85 = 0L
        var estimatedQ75 = 0L

        val maxBytesQ100 = (1.5 * 1024 * 1024).toLong()
        val maxBytesQ85 = (0.4 * 1024 * 1024).toLong()
        val maxBytesQ75 = (0.15 * 1024 * 1024).toLong()

        photos.forEach { photo ->
            val size = getPhotoFileSize(context, photo.filePath)
            if (photo.filePath.endsWith(".mp4", ignoreCase = true)) {
                totalVideoBytes += size
                videoCount++
            } else {
                totalPhotoBytes += size
                photoCount++

                estimatedQ100 += minOf(size, maxBytesQ100)
                estimatedQ85 += minOf(size, maxBytesQ85)
                estimatedQ75 += minOf(size, maxBytesQ75)
            }
        }

        FileSizeInfo(
            totalPhotoBytes = totalPhotoBytes,
            totalVideoBytes = totalVideoBytes,
            photoCount = photoCount,
            videoCount = videoCount,
            estimatedQ100Bytes = estimatedQ100,
            estimatedQ85Bytes = estimatedQ85,
            estimatedQ75Bytes = estimatedQ75,
        )
    }

    private fun getPhotoFileSize(context: Context, filePath: String): Long {
        return try {
            val uri = filePath.toUri()
            if (filePath.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    it.statSize
                } ?: 0L
            } else {
                val path = if (filePath.startsWith("file://")) uri.path else filePath
                path?.let { File(it).length() } ?: 0L
            }
        } catch (e: java.io.IOException) {
            Log.w("ReportRepo", "Failed to get size for $filePath", e)
            0L
        } catch (e: SecurityException) {
            Log.w("ReportRepo", "Storage access denied for $filePath", e)
            0L
        } catch (e: android.os.RemoteException) {
            Log.w("ReportRepo", "Remote process error for $filePath", e)
            0L
        }
    }
}
