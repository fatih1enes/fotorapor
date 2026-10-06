package com.fatihenes.photoreport.feature.camera.engine

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodecList
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

            // Ultra-wide detection: minZoom < 1.0 indicates ultra-wide lens (0.5x, 0.6x, etc.)
            val supportsUltraWide = minZoom < 1f
            val ultraWideMinZoom = if (supportsUltraWide) minZoom else 1f

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

            // Zero Shutter Lag (ZSL) — requires FULL+ hardware AND either the
            // PRIVATE_REPROCESSING or YUV_REPROCESSING capability bit. Relying
            // on hardware level alone produces false-positives on LIMITED devices
            // that expose a FULL-level ISP but no reprocessing pipeline.
            val availCaps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
            val hasReprocessing =
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING in availCaps ||
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING in availCaps
            val supportsZsl = isFullOrBetter && hasReprocessing

            // Low-light boost: some OEMs expose a Night AE mode (value 5) even
            // without the Night extension, giving free low-light SNR improvement.
            val aeModes = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) ?: intArrayOf()
            @Suppress("MagicNumber")
            val supportsNightAe = 5 in aeModes // CaptureRequest.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST

            // HEIC support: Android 10+ with HEIC encoder
            val supportsHeic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasHeicEncoder()

            // HDR video support: Android 13+ (Tiramisu) with HDR video capabilities
            val supportsHdrVideo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                cameraInfo != null &&
                try {
                    // Check if HDR dynamic range is supported for video recording
                    val videoCaps = Recorder.getVideoCapabilities(cameraInfo)
                    val supportedRanges = videoCaps.supportedDynamicRanges
                    supportedRanges.size > 1 // More than just SDR means HDR/HLG support
                } catch (e: Exception) {
                    false
                }

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
                supportsLowLightBoost = supportsNightAe,
                supportedVideoQualities = qualities,
                supportsHdrExtension = supportsHdr,
                supportsNightExtension = supportsNight,
                supportsHeic = supportsHeic,
                supportsHdrVideo = supportsHdrVideo,
                supportsUltraWide = supportsUltraWide,
                ultraWideMinZoomRatio = ultraWideMinZoom
            ).also {
                Log.d(TAG, "Queried capabilities for lens $lensFacing: $it")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query camera capabilities for lens $lensFacing", e)
            EnhancedCapabilities()
        }
    }

    private fun hasHeicEncoder(): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { info ->
                info.isEncoder && "image/heic".equals(info.getSupportedTypes().firstOrNull(), ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache() {
        capabilitiesCache.clear()
    }
}
