package com.fatihenes.photoreport.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Centralized image utility functions.
 * Consolidates duplicated logic from PdfExporter and HtmlExporter (DRY principle).
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * Reads EXIF rotation from an image file.
     * Supports content://, file://, and absolute path strings.
     * Returns 0f for videos or on failure.
     */
    fun getExifRotation(context: Context, pathString: String): Float {
        if (pathString.endsWith(".mp4", ignoreCase = true)) return 0f
        return try {
            val uri = pathString.toUri()
            val exif = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ExifInterface(inputStream)
            } ?: return 0f
            parseExifOrientation(exif)
        } catch (_: Exception) {
            try {
                val exif = ExifInterface(pathString)
                parseExifOrientation(exif)
            } catch (_: Exception) {
                0f
            }
        }
    }

    private fun parseExifOrientation(exif: ExifInterface): Float {
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    /**
     * Loads a bitmap from a path string, downsampled to fit within [targetWidth] x [targetHeight].
     * Uses two-pass decoding (bounds-only first) for memory safety.
     * Falls back to direct file path if ContentResolver fails.
     */
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

        // First pass: decode bounds only
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream1 = context.contentResolver.openInputStream(uri) ?: return null
        stream1.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        }

        val sampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)

        // Second pass: decode with sample size
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

    /**
     * Calculates the optimal inSampleSize for downsampling.
     */
    private fun calculateInSampleSize(
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
        // OOM Koruma Tavanı: Maksimum ~3 Megabayt piksel (ARGB_8888 ile en fazla ~12MB RAM)
        while ((height / inSampleSize) * (width / inSampleSize) > 3_000_000) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /**
     * Compresses an image and saves it to [destFile] with the given [quality].
     * Handles EXIF rotation, downscaling to [maxDimension], and memory-safe decoding.
     * Returns true on success, false on failure.
     */
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

    /**
     * Compresses an image and writes directly into an OutputStream (e.g. ZipOutputStream) without temporary disk files.
     */
    fun compressToStream(
        context: Context,
        pathString: String,
        outputStream: java.io.OutputStream,
        quality: Int,
        maxDimension: Int = 2000
    ): Boolean {
        return try {
            // First pass: decode bounds only
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val stream1 = openInputStreamSafe(context, pathString) ?: return false
            stream1.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)

            // Second pass: decode with calculated sample size
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inMutable = true
            }
            val stream2 = openInputStreamSafe(context, pathString) ?: return false
            val originalBitmap = stream2.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return false

            // Apply EXIF rotation only if non-zero
            val exifRotation = getExifRotation(context, pathString)
            val bitmap = if (exifRotation != 0f) {
                val matrix = android.graphics.Matrix().apply { postRotate(exifRotation) }
                val rotated = Bitmap.createBitmap(
                    originalBitmap, 0, 0,
                    originalBitmap.width, originalBitmap.height,
                    matrix, true
                )
                if (rotated != originalBitmap && !originalBitmap.isRecycled) originalBitmap.recycle()
                rotated
            } else {
                originalBitmap
            }

            // Scale down if still too large
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

            if (finalBitmap != bitmap && !bitmap.isRecycled) bitmap.recycle()
            if (!finalBitmap.isRecycled) finalBitmap.recycle()
            true
        } catch (e: Exception) {
            Log.e(TAG, "compressToStream failed", e)
            false
        }
    }

    /**
     * Compresses an image and saves it to [destFile] with the given [quality].
     */
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
