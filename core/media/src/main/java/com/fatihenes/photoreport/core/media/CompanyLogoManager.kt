package com.fatihenes.photoreport.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object CompanyLogoManager {
    private const val LOGO_FILE_NAME = "company_logo.png"
    private const val TAG = "CompanyLogoManager"

    private fun getLogoFile(context: Context): File {
        return File(context.filesDir, LOGO_FILE_NAME)
    }

    suspend fun saveLogo(
        context: Context,
        bitmap: Bitmap,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) = withContext(dispatcher) {
        val file = getLogoFile(context)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun getLogoUri(context: Context): Uri? {
        val file = getLogoFile(context)
        if (!file.exists()) return null
        return Uri.fromFile(file)
    }

    fun hasLogo(context: Context): Boolean {
        return getLogoFile(context).exists()
    }

    suspend fun deleteLogo(
        context: Context,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) = withContext(dispatcher) {
        val file = getLogoFile(context)
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Failed to delete logo file: ${file.path}")
        }
    }
}
