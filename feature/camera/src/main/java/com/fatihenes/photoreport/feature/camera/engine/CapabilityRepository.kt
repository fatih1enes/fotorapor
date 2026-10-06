package com.fatihenes.photoreport.feature.camera.engine

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.core.DynamicRange
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CapabilityRepository"

@Singleton
class CapabilityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    private val capabilitiesCache = ConcurrentHashMap<Int, EnhancedCapabilities>()

    suspend fun getCapabilities(
        lensFacing: Int,
        cameraInfo: CameraInfo? = null,
        extensionsManager: ExtensionsManager? = null
    ): EnhancedCapabilities = withContext(ioDispatcher) {
        capabilitiesCache[lensFacing]?.let { return@withContext it }

        val queried = queryCapabilities(lensFacing, cameraInfo, extensionsManager)
        capabilitiesCache[lensFacing] = queried
        queried
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun queryCapabilities(
        lensFacing: Int,
        cameraInfo: CameraInfo?,
        extensionsManager: ExtensionsManager?
    ): EnhancedCapabilities {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val desiredFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }

            val cameraId = cm.cameraIdList.firstOrNull { id ->
                try {
                    cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == desiredFacing
                } catch (e: Exception) {
                    false
                }
            } ?: cm.cameraIdList.firstOrNull() ?: return EnhancedCapabilities()

            val chars = cm.getCameraCharacteristics(cameraId)
            val hwLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

            val isLegacy = hwLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
            val isFullOrBetter = hwLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hwLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3

            // Stabilization
            val stabModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: intArrayOf()
            val supportsPreviewStab = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION in stabModes
            } else false
            val supportsVideoStab = CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON in stabModes

            // OIS
            val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: intArrayOf()
            val supportsOis = CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON in oisModes

            // Flash unit
            val hasFlashUnit = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            // Zoom range
            var minZoom = 1f
            var maxZoom = 8f
            cameraInfo?.zoomState?.value?.let { z ->
                minZoom = z.minZoomRatio
                maxZoom = z.maxZoomRatio
            }

            // Exposure
            var lowerExp = 0
            var upperExp = 0
            var expStep = 0f
            cameraInfo?.exposureState?.let { s ->
                lowerExp = s.exposureCompensationRange.lower
                upperExp = s.exposureCompensationRange.upper
                expStep = s.exposureCompensationStep.toFloat()
            } ?: run {
                val expRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                if (expRange != null) {
                    lowerExp = expRange.lower
                    upperExp = expRange.upper
                    val stepRational = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                    if (stepRational != null) {
                        expStep = stepRational.toFloat()
                    }
                }
            }

            // Supported video qualities
            val qualities: List<Quality> = if (cameraInfo != null) {
                try {
                    Recorder.getVideoCapabilities(cameraInfo).getSupportedQualities(DynamicRange.SDR).toList()
                } catch (e: Exception) {
                    listOf(Quality.SD, Quality.HD, Quality.FHD)
                }
            } else {
                listOf(Quality.SD, Quality.HD, Quality.FHD)
            }

            // Extension checks
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val supportsHdr = extensionsManager?.isExtensionAvailable(selector, ExtensionMode.HDR) ?: false
            val supportsNight = extensionsManager?.isExtensionAvailable(selector, ExtensionMode.NIGHT) ?: false

            // Zero shutter lag
            val supportsZsl = isFullOrBetter

            EnhancedCapabilities(
                hardwareLevel = hwLevel,
                isLegacy = isLegacy,
                isFullOrBetter = isFullOrBetter,
                minZoomRatio = minZoom,
                maxZoomRatio = maxZoom,
                exposureRangeLower = lowerExp,
                exposureRangeUpper = upperExp,
                exposureStep = expStep,
                supportsPreviewStabilization = supportsPreviewStab,
                supportsVideoStabilization = supportsVideoStab,
                supportsOis = supportsOis,
                hasFlashUnit = hasFlashUnit,
                supportsTorch = hasFlashUnit,
                supportsZeroShutterLag = supportsZsl,
                supportsLowLightBoost = false,
                supportedVideoQualities = qualities,
                supportsHdrExtension = supportsHdr,
                supportsNightExtension = supportsNight
            ).also {
                Log.d(TAG, "Queried capabilities for lens $lensFacing: $it")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query camera capabilities for lens $lensFacing", e)
            EnhancedCapabilities()
        }
    }

    fun clearCache() {
        capabilitiesCache.clear()
    }
}
