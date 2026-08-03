package com.fatihenes.photoreport.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Data class representing watermark information to overlay on captured photos.
 */
data class WatermarkData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val dateTime: String = "",
    val projectName: String = ""
)

/**
 * Lightweight location utility for GPS watermark feature.
 * Uses FusedLocationProviderClient for efficient battery-friendly location retrieval.
 * Falls back gracefully if permissions are denied or location is unavailable.
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocationManager"
    }

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Checks if location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retrieves the current location, or null if unavailable.
     * Uses withTimeout to prevent hanging in poor signal areas.
     */
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null

        try {
            // Priority 1: Fresh location with strict timeout (10 seconds)
            val freshLocation = kotlinx.coroutines.withTimeoutOrNull(kotlin.time.Duration.parse("10s")) {
                getFreshLocation()
            }
            if (freshLocation != null) return@withContext freshLocation

            // Priority 2: Last known location as fallback
            getLastKnownLocation()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get location safely", e)
            null
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getFreshLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cancellationTokenSource = CancellationTokenSource()
        cont.invokeOnCancellation { cancellationTokenSource.cancel() }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            cont.resume(location)
        }.addOnFailureListener {
            cont.resume(null)
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location -> cont.resume(location) }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * Reverse-geocodes coordinates to a human-readable address.
     * Returns null if geocoding fails or is unavailable.
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext null
                val geocoder = Geocoder(context, Locale.getDefault())

                val address: android.location.Address? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                Log.w(TAG, "Geocoding onError: $errorMessage")
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()
                }

                address?.let { addr ->
                    buildString {
                        addr.thoroughfare?.let { append(it) }
                        addr.subThoroughfare?.let { if (isNotEmpty()) append(" "); append(it) }
                        addr.subLocality?.let { if (isNotEmpty()) append(", "); append(it) }
                        addr.locality?.let { if (isNotEmpty()) append(", "); append(it) }
                        addr.adminArea?.let { if (isNotEmpty()) append(", "); append(it) }
                    }.ifEmpty { null }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoding failed", e)
                null
            }
        }
    }

    /**
     * Builds a complete WatermarkData object by fetching current GPS + address.
     */
    suspend fun buildWatermarkData(projectName: String): WatermarkData {
        val now = java.time.LocalDateTime.now()
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
        val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
        val dateTimeString = "${now.format(dateFormatter)} ${now.format(timeFormatter)}"

        val location = getCurrentLocation()
        val address = location?.let { getAddressFromLocation(it.latitude, it.longitude) }

        return WatermarkData(
            latitude = location?.latitude,
            longitude = location?.longitude,
            address = address,
            dateTime = dateTimeString,
            projectName = projectName
        )
    }
}
