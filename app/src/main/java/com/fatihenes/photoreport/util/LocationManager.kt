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
import com.fatihenes.photoreport.core.model.WatermarkData
import kotlin.coroutines.resume

/**
 * Lightweight location utility for GPS watermark feature.
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

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @android.annotation.SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null

        try {
            val freshLocation = kotlinx.coroutines.withTimeoutOrNull(kotlin.time.Duration.parse("10s")) {
                getFreshLocation()
            }
            freshLocation ?: getLastKnownLocation()
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
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val address = fetchAddress(latitude, longitude)
            address?.let { formatAddress(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoding failed", e)
            null
        }
    }

    private suspend fun fetchAddress(lat: Double, lng: Double): android.location.Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            fetchAddressTiramisu(geocoder, lat, lng)
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
        }
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.TIRAMISU)
    private suspend fun fetchAddressTiramisu(geocoder: Geocoder, lat: Double, lng: Double): android.location.Address? =
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    if (cont.isActive) cont.resume(addresses.firstOrNull())
                }
                override fun onError(errorMessage: String?) {
                    Log.w(TAG, "Geocoding onError: $errorMessage")
                    if (cont.isActive) cont.resume(null)
                }
            })
        }

    private fun formatAddress(addr: android.location.Address): String? {
        val parts = listOfNotNull(
            addr.thoroughfare,
            addr.subThoroughfare,
            addr.subLocality,
            addr.locality,
            addr.adminArea
        )
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }

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
