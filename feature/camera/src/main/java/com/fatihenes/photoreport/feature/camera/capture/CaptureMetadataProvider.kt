package com.fatihenes.photoreport.feature.camera.capture

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import com.fatihenes.photoreport.core.model.WatermarkData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "CaptureMetadataProvider"
private const val GPS_GEOCODER_TIMEOUT_MS = 2500L

@Singleton
class CaptureMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    @SuppressLint("MissingPermission")
    suspend fun createSnapshot(
        projectName: String,
        includeGps: Boolean
    ): WatermarkData = withContext(ioDispatcher) {
        val now = Date()
        val formattedDate = dateFormat.format(now)

        if (!includeGps) {
            return@withContext WatermarkData(
                latitude = null,
                longitude = null,
                address = null,
                dateTime = formattedDate,
                projectName = projectName
            )
        }

        val location = getBestLastKnownLocation()
        val addressText = location?.let { getAddressFromLocation(it.latitude, it.longitude) }

        WatermarkData(
            latitude = location?.latitude,
            longitude = location?.longitude,
            address = addressText,
            dateTime = formattedDate,
            projectName = projectName
        )
    }

    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            bestLocation
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get location for capture snapshot", e)
            null
        }
    }

    private suspend fun getAddressFromLocation(
        lat: Double,
        lng: Double
    ): String? = withTimeoutOrNull(GPS_GEOCODER_TIMEOUT_MS) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val addr = addresses.firstOrNull()
                            val line = addr?.getAddressLine(0)
                            val formatted = if (!line.isNullOrBlank()) {
                                line
                            } else if (addr != null) {
                                "${addr.subAdminArea.orEmpty()}, ${addr.adminArea.orEmpty()}"
                            } else null
                            if (cont.isActive) cont.resume(formatted)
                        }

                        override fun onError(errorMessage: String?) {
                            Log.w(TAG, "Geocoder error: $errorMessage")
                            if (cont.isActive) cont.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lng, 1)
                list?.firstOrNull()?.let { addr ->
                    val line = addr.getAddressLine(0)
                    if (!line.isNullOrBlank()) line
                    else "${addr.subAdminArea.orEmpty()}, ${addr.adminArea.orEmpty()}"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder failed for ($lat, $lng)", e)
            null
        }
    }
}
