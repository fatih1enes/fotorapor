package com.fatihenes.photoreport.core.media

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object ImageProcessor {
    fun getExifRotation(context: Context, pathString: String): Float =
        MetadataReader.getExifRotation(context, pathString)

    fun loadScaledBitmap(
        context: Context,
        pathString: String,
        targetWidth: Int,
        targetHeight: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ): Bitmap? = ImageScaler.loadScaledBitmap(context, pathString, targetWidth, targetHeight, config)

    fun openInputStreamSafe(context: Context, pathString: String): InputStream? =
        ImageCompressor.openInputStreamSafe(context, pathString)

    fun compressToStream(
        context: Context,
        pathString: String,
        outputStream: OutputStream,
        quality: Int,
        maxDimension: Int = 2000
    ): Boolean = ImageCompressor.compressToStream(context, pathString, outputStream, quality, maxDimension)

    fun compressAndSaveImage(
        context: Context,
        pathString: String,
        destFile: File,
        quality: Int,
        maxDimension: Int = 2000
    ): Boolean = ImageCompressor.compressAndSaveImage(context, pathString, destFile, quality, maxDimension)
}
