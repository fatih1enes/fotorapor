package com.fatihenes.photoreport.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object CompanyLogoManager {
    private const val LOGO_FILE_NAME = "company_logo.png"

    private fun getLogoFile(context: Context): File {
        return File(context.filesDir, LOGO_FILE_NAME)
    }

    fun saveLogo(context: Context, bitmap: Bitmap) {
        val file = getLogoFile(context)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun getLogoUri(context: Context): Uri? {
        val file = getLogoFile(context)
        if (!file.exists()) return null
        // DÃƒÆ’Ã‚Â¶nÃƒÆ’Ã‚Â¼Ãƒâ€¦Ã…Â¸ olarak FileProvider URI'si verebiliriz, ya da direkt file URI verebiliriz
        // Glide ve AsyncImage iÃƒÆ’Ã‚Â§in file:// yeterli
        return Uri.fromFile(file)
    }

    fun getLogoBitmap(context: Context): Bitmap? {
        val file = getLogoFile(context)
        if (!file.exists()) return null
        // OOM önlemi: Logo küçük olsa bile güvenlik amaçlı en fazla 512x512 yüklüyoruz
        return ImageUtils.loadScaledBitmap(context, file.absolutePath, 512, 512)
    }

    fun hasLogo(context: Context): Boolean {
        return getLogoFile(context).exists()
    }

    fun deleteLogo(context: Context) {
        val file = getLogoFile(context)
        if (file.exists()) {
            file.delete()
        }
    }
}
