package com.fatihenes.photoreport.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.fatihenes.photoreport.core.model.PhotoCropState

object PhotoCropRenderer {

    /**
     * Applies crop, rotation, flip, and straighten to a bitmap and returns the transformed Bitmap.
     * Recycles the intermediate bitmaps safely.
     */
    fun applyCropAndTransform(source: Bitmap, cropState: PhotoCropState): Bitmap {
        if (cropState.isDefault()) return source

        var current = source

        // 1. Apply 90-degree step rotations and flips first
        if (cropState.rotationDegrees != 0 || cropState.isFlippedHorizontal || cropState.isFlippedVertical || cropState.straightenAngle != 0f) {
            val matrix = Matrix()

            if (cropState.isFlippedHorizontal) {
                matrix.postScale(-1f, 1f, current.width / 2f, current.height / 2f)
            }
            if (cropState.isFlippedVertical) {
                matrix.postScale(1f, -1f, current.width / 2f, current.height / 2f)
            }

            val totalRotation = (cropState.rotationDegrees.toFloat() + cropState.straightenAngle)
            if (totalRotation != 0f) {
                matrix.postRotate(totalRotation, current.width / 2f, current.height / 2f)
            }

            val transformed = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
            if (transformed !== current && current !== source) {
                current.recycle()
            }
            current = transformed
        }

        // 2. Apply Crop rectangle
        if (cropState.cropLeft > 0f || cropState.cropTop > 0f || cropState.cropRight < 1f || cropState.cropBottom < 1f) {
            val srcW = current.width
            val srcH = current.height

            val left = (cropState.cropLeft * srcW).toInt().coerceIn(0, srcW - 1)
            val top = (cropState.cropTop * srcH).toInt().coerceIn(0, srcH - 1)
            val right = (cropState.cropRight * srcW).toInt().coerceIn(left + 1, srcW)
            val bottom = (cropState.cropBottom * srcH).toInt().coerceIn(top + 1, srcH)
            val cropW = right - left
            val cropH = bottom - top

            if (cropW > 0 && cropH > 0) {
                val cropped = Bitmap.createBitmap(current, left, top, cropW, cropH)
                if (cropped !== current && current !== source) {
                    current.recycle()
                }
                current = cropped
            }
        }

        return current
    }
}
