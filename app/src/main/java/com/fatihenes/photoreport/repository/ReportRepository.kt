package com.fatihenes.photoreport.repository

import android.content.Context
import androidx.core.net.toUri
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.ui.viewmodel.FileSizeInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ReportRepository {
    suspend fun calculateFileSizes(photos: List<PhotoEntity>): FileSizeInfo
}

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReportRepository {

    override suspend fun calculateFileSizes(photos: List<PhotoEntity>): FileSizeInfo = withContext(Dispatchers.IO) {
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
            try {
                val uri = photo.filePath.toUri()
                val size = if (photo.filePath.startsWith("content://")) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use {
                        it.statSize
                    } ?: 0L
                } else {
                    val path = if (photo.filePath.startsWith("file://")) uri.path else photo.filePath
                    path?.let { File(it).length() } ?: 0L
                }

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
            } catch (_: Exception) {}
        }

        FileSizeInfo(
            totalPhotoBytes,
            totalVideoBytes,
            photoCount,
            videoCount,
            estimatedQ100,
            estimatedQ85,
            estimatedQ75
        )
    }
}
