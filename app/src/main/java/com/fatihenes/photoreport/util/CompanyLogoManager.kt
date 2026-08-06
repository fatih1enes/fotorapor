package com.fatihenes.photoreport.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

object CompanyLogoManager {
    suspend fun saveLogo(context: Context, bitmap: Bitmap) =
        com.fatihenes.photoreport.core.media.CompanyLogoManager.saveLogo(context, bitmap)

    fun getLogoUri(context: Context): Uri? =
        com.fatihenes.photoreport.core.media.CompanyLogoManager.getLogoUri(context)

    fun hasLogo(context: Context): Boolean =
        com.fatihenes.photoreport.core.media.CompanyLogoManager.hasLogo(context)

    suspend fun deleteLogo(context: Context) =
        com.fatihenes.photoreport.core.media.CompanyLogoManager.deleteLogo(context)
}
