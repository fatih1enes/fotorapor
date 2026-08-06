package com.fatihenes.photoreport.feature.camera.model

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Log
import androidx.camera.core.CameraSelector

private const val TAG = "CameraCapabilities"

/**
 * Holds the actual hardware capabilities of a specific camera device,
 * queried directly from [CameraCharacteristics].
 */
data class CameraCapabilities(
    val hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
    val supportsEdgeHighQuality: Boolean = false,
    val supportsNoiseReductionHighQuality: Boolean = false,
    val supportsPreviewStabilization: Boolean = false,
    val supportsOis: Boolean = false,
) {
    val isLegacy get() = hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    val isFullOrBetter get() = hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
            || hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
}

/**
 * Query real capabilities from Camera2 for the requested lens.
 * Returns a safe default (everything disabled) if anything goes wrong.
 */
fun queryCameraCapabilities(context: Context, lensFacing: Int): CameraCapabilities {
    return try {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val desiredFace = if (lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT

        val cameraId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == desiredFace
        } ?: cm.cameraIdList.firstOrNull() ?: return CameraCapabilities()

        val chars = cm.getCameraCharacteristics(cameraId)
        val hwLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

        // Edge mode support
        val edgeModes = chars.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES) ?: intArrayOf()
        val supportsEdgeHQ = CaptureRequest.EDGE_MODE_HIGH_QUALITY in edgeModes

        // Noise reduction support
        val nrModes = chars.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES) ?: intArrayOf()
        val supportsNrHQ = CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY in nrModes

        // Preview stabilization (available since API 33 / CameraX 1.3+)
        val stabilizationModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: intArrayOf()
        val supportsPreviewStab = if (android.os.Build.VERSION.SDK_INT >= 33) {
            CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION in stabilizationModes
        } else {
            false
        }

        // Optical Image Stabilization
        val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: intArrayOf()
        val supportsOis = CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON in oisModes

        CameraCapabilities(
            hardwareLevel = hwLevel,
            supportsEdgeHighQuality = supportsEdgeHQ,
            supportsNoiseReductionHighQuality = supportsNrHQ,
            supportsPreviewStabilization = supportsPreviewStab,
            supportsOis = supportsOis,
        ).also {
            Log.d(TAG, "Capabilities: hw=$hwLevel, edge=$supportsEdgeHQ, nr=$supportsNrHQ, prevStab=$supportsPreviewStab, ois=$supportsOis")
        }
    } catch (e: Exception) {
        Log.w(TAG, "Capability query failed — using safe defaults", e)
        CameraCapabilities()
    }
}
