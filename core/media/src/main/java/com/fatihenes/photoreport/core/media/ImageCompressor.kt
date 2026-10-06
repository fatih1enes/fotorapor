@file:Suppress("TooGenericExceptionCaught")
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
        var originalBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        var finalBitmap: Bitmap? = null
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
            originalBitmap = stream2.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return false

            val exifRotation = MetadataReader.getExifRotation(context, pathString)
            rotatedBitmap = ImageRotation.rotateIfNeeded(originalBitmap, exifRotation)

            finalBitmap = if (rotatedBitmap.width > maxDimension || rotatedBitmap.height > maxDimension) {
                val ratio = rotatedBitmap.width.toFloat() / rotatedBitmap.height.toFloat()
                val (targetW, targetH) = if (ratio > 1) {
                    maxDimension to (maxDimension / ratio).toInt()
                } else {
                    (maxDimension * ratio).toInt() to maxDimension
                }
                rotatedBitmap.scale(targetW, targetH, filter = true)
            } else {
                rotatedBitmap
            }

            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "compressToStream failed", e)
            false
        } finally {
            if (originalBitmap !== rotatedBitmap && originalBitmap?.isRecycled == false) {
                originalBitmap?.recycle()
            }
            if (rotatedBitmap !== finalBitmap && rotatedBitmap?.isRecycled == false) {
                rotatedBitmap?.recycle()
            }
            if (finalBitmap?.isRecycled == false) {
                finalBitmap?.recycle()
            }
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
