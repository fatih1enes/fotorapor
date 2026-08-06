package com.fatihenes.photoreport.util

import android.content.Context
import android.net.Uri
import com.fatihenes.photoreport.core.model.WatermarkData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatermarkRenderer @Inject constructor(
    private val coreRenderer: com.fatihenes.photoreport.core.media.WatermarkRenderer
) {
    suspend fun applyWatermark(
        context: Context,
        originalUri: Uri,
        watermarkData: WatermarkData
    ): Uri = coreRenderer.applyWatermark(context, originalUri, watermarkData)
}
