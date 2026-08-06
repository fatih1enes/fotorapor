package com.fatihenes.photoreport.util

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaProcessor @Inject constructor(
    private val coreProcessor: com.fatihenes.photoreport.core.media.MediaProcessor
) {
    suspend fun processAndOptimize(
        originalUri: Uri,
        enableAvif: Boolean,
        projectName: String
    ): Uri = coreProcessor.processAndOptimize(originalUri, enableAvif, projectName)
}
