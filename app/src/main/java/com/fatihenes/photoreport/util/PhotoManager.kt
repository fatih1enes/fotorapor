package com.fatihenes.photoreport.util

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.video.MediaStoreOutputOptions

object PhotoManager {
    fun getCaptureOutputOptions(context: Context, lensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK): ImageCapture.OutputFileOptions =
        com.fatihenes.photoreport.core.media.PhotoManager.getCaptureOutputOptions(context, lensFacing)

    fun getVideoOutputOptions(context: Context): MediaStoreOutputOptions =
        com.fatihenes.photoreport.core.media.PhotoManager.getVideoOutputOptions(context)

    fun copyUriToInternalStorage(context: Context, uri: Uri): Uri? =
        com.fatihenes.photoreport.core.media.PhotoManager.copyUriToInternalStorage(context, uri)
}
