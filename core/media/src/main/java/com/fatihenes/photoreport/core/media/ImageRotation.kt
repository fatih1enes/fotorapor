package com.fatihenes.photoreport.core.media

import android.graphics.Bitmap
import android.graphics.Matrix

object ImageRotation {
    fun rotateIfNeeded(originalBitmap: Bitmap, rotationDegrees: Float): Bitmap {
        if (rotationDegrees == 0f) return originalBitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotated = Bitmap.createBitmap(
            originalBitmap, 0, 0,
            originalBitmap.width, originalBitmap.height,
            matrix, true
        )
        if (rotated !== originalBitmap && !originalBitmap.isRecycled) {
            originalBitmap.recycle()
        }
        return rotated
    }
}
