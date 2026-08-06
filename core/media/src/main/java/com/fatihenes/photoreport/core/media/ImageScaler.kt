package com.fatihenes.photoreport.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.net.toUri

object ImageScaler {
    private const val TAG = "ImageScaler"

    fun loadScaledBitmap(
        context: Context,
        pathString: String,
        targetWidth: Int,
        targetHeight: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ): Bitmap? {
        return try {
            loadScaledBitmapViaContentResolver(context, pathString, targetWidth, targetHeight, config)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM in loadScaledBitmap", e)
            null
        } catch (e: Exception) {
            loadScaledBitmapViaFile(pathString, targetWidth, targetHeight, config)
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
