package com.fatihenes.photoreport.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object ImageCompressor {
    private const val TAG = "ImageCompressor"

    fun openInputStreamSafe(context: Context, pathString: String): InputStream? {
        return try {
            val uri = pathString.toUri()
            if (uri.scheme == "content" || uri.scheme == "file") {
                context.contentResolver.openInputStream(uri)
            } else {
                val f = File(pathString)
                if (f.exists()) f.inputStream() else null
            }
        } catch (e: Exception) {
            val uri = pathString.toUri()
            val f = File(uri.path ?: pathString)
            if (f.exists()) f.inputStream() else null
        }
    }

    fun compressToStream(
        context: Context,
        pathString: String,
        outputStream: OutputStream,
        quality: Int,
        maxDimension: Int = 2000
    ): Boolean {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val stream1 = openInputStreamSafe(context, pathString) ?: return false
            stream1.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val inSampleSize = ImageScaler.calculateInSampleSize(options, maxDimension, maxDimension)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inMutable = true
            }
            val stream2 = openInputStreamSafe(context, pathString) ?: return false
            val originalBitmap = stream2.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return false

            val exifRotation = MetadataReader.getExifRotation(context, pathString)
            val bitmap = ImageRotation.rotateIfNeeded(originalBitmap, exifRotation)

            val finalBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val (targetW, targetH) = if (ratio > 1) {
                    maxDimension to (maxDimension / ratio).toInt()
                } else {
                    (maxDimension * ratio).toInt() to maxDimension
                }
                val scaled = bitmap.scale(targetW, targetH, filter = true)
                if (scaled !== bitmap && !bitmap.isRecycled) bitmap.recycle()
                scaled
            } else {
                bitmap
            }

            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (finalBitmap !== bitmap && !bitmap.isRecycled) bitmap.recycle()
            if (!finalBitmap.isRecycled) finalBitmap.recycle()
            true
        } catch (e: Exception) {
            Log.e(TAG, "compressToStream failed", e)
            false
        }
    }

    fun compressAndSaveImage(
        context: Context,
        pathString: String,
        destFile: File,
        quality: Int,
        maxDimension: Int = 2000
    ): Boolean {
        return try {
            FileOutputStream(destFile).use { output ->
                compressToStream(context, pathString, output, quality, maxDimension)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed to save file", e)
            false
        }
    }
}
