package com.fatihenes.photoreport.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface

object ImageScaler {
    private const val TAG = "ImageScaler"
    private const val ROTATION_90 = 90f
    private const val ROTATION_180 = 180f
    private const val ROTATION_270 = 270f

    fun loadScaledBitmap(
        context: Context,
        pathString: String,
        targetWidth: Int,
        targetHeight: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ): Bitmap? {
        val bitmap = try {
            loadScaledBitmapViaContentResolver(context, pathString, targetWidth, targetHeight, config)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM in loadScaledBitmap", e)
            null
        } catch (e: Exception) {
            loadScaledBitmapViaFile(pathString, targetWidth, targetHeight, config)
        } ?: return null

        return rotateBitmapIfNeeded(context, pathString, bitmap)
    }

    @Suppress("ReturnCount")
    private fun rotateBitmapIfNeeded(context: Context, pathString: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = if (pathString.startsWith("content://")) {
                context.contentResolver.openInputStream(pathString.toUri())?.use { ExifInterface(it) }
            } else {
                ExifInterface(pathString)
            }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(ROTATION_90)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(ROTATION_180)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(ROTATION_270)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            Log.w(TAG, "Exif rotation failed", e)
            bitmap
        }
    }

    private fun loadScaledBitmapViaContentResolver(
        context: Context,
        pathString: String,
        targetWidth: Int,
        targetHeight: Int,
        config: Bitmap.Config
    ): Bitmap? {
        val uri = pathString.toUri()
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream1 = context.contentResolver.openInputStream(uri) ?: return null
        stream1.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        }
        val sampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
        val stream2 = context.contentResolver.openInputStream(uri) ?: return null
        return stream2.use { inputStream ->
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = config
                inMutable = true
            }
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        }
    }

    private fun loadScaledBitmapViaFile(
        pathString: String,
        targetWidth: Int,
        targetHeight: Int,
        config: Bitmap.Config
    ): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(pathString, boundsOptions)
            val sampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = config
                inMutable = true
            }
            BitmapFactory.decodeFile(pathString, decodeOptions)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM in fallback loadScaledBitmap", e)
            null
        } catch (e: Exception) {
            null
        }
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var inSampleSize = 1
        val height = options.outHeight
        val width = options.outWidth
        if (height > targetHeight || width > targetWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                inSampleSize *= 2
            }
        }
        while ((height / inSampleSize) * (width / inSampleSize) > 3_000_000) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
