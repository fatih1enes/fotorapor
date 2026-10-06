package com.fatihenes.photoreport.feature.camera.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

enum class GpsStatus {
    DISABLED,
    NO_PERMISSION,
    PROVIDER_OFF,
    SEARCHING,
    ACQUIRED
}

@Stable
class LocationMonitorState(
    val status: State<GpsStatus>,
    val location: State<Location?>,
    val accuracyMeters: State<Float?>,
    val displayText: State<String?>
)

private const val LOCATION_UPDATE_INTERVAL_MS = 2000L
private const val LOCATION_MIN_DISTANCE_M = 2f

@SuppressLint("MissingPermission")
private fun findBestLastKnownLocation(lm: LocationManager): Location? {
    val providers = lm.getProviders(true)
    var bestLoc: Location? = null
    for (provider in providers) {
        val loc = try {
            lm.getLastKnownLocation(provider)
        } catch (_: Exception) {
            null
        }
        if (loc != null && (bestLoc == null || loc.accuracy < bestLoc.accuracy)) {
            bestLoc = loc
        }
    }
    return bestLoc
}

@SuppressLint("MissingPermission")
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun rememberLocationMonitor(
    context: Context,
    enabled: Boolean
): LocationMonitorState {
    val statusState = remember { mutableStateOf(if (enabled) GpsStatus.SEARCHING else GpsStatus.DISABLED) }
    val locationState = remember { mutableStateOf<Location?>(null) }
    val accuracyState = remember { mutableStateOf<Float?>(null) }
    val textState = remember { mutableStateOf<String?>(null) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            statusState.value = GpsStatus.DISABLED
            locationState.value = null
            accuracyState.value = null
            textState.value = "● GPS Kapalı"
            return@DisposableEffect onDispose {}
        }

        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            statusState.value = GpsStatus.NO_PERMISSION
            locationState.value = null
            accuracyState.value = null
            textState.value = "● Konum İzni Yok"
            return@DisposableEffect onDispose {}
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null || !LocationManagerCompat.isLocationEnabled(lm)) {
            statusState.value = GpsStatus.PROVIDER_OFF
            locationState.value = null
            accuracyState.value = null
            textState.value = "● Cihaz Konumu Kapalı"
            return@DisposableEffect onDispose {}
        }

        // Initially check last known location for quick response
        val bestLoc = findBestLastKnownLocation(lm)
        if (bestLoc != null) {
            locationState.value = bestLoc
            accuracyState.value = bestLoc.accuracy
            statusState.value = GpsStatus.ACQUIRED
            textState.value = "● GPS ±${bestLoc.accuracy.toInt()}m"
        } else {
            statusState.value = GpsStatus.SEARCHING
            textState.value = "● Konum Aranıyor..."
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                locationState.value = loc
                accuracyState.value = loc.accuracy
                statusState.value = GpsStatus.ACQUIRED
                textState.value = "● GPS ±${loc.accuracy.toInt()}m"
            }

            override fun onProviderEnabled(provider: String) {
                if (statusState.value == GpsStatus.PROVIDER_OFF) {
                    statusState.value = GpsStatus.SEARCHING
                    textState.value = "● Konum Aranıyor..."
                }
            }

            override fun onProviderDisabled(provider: String) {
                if (!LocationManagerCompat.isLocationEnabled(lm)) {
                    statusState.value = GpsStatus.PROVIDER_OFF
                    locationState.value = null
                    accuracyState.value = null
                    textState.value = "● Cihaz Konumu Kapalı"
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Compatibility callback for older Android APIs
            }
        }

        val registeredProviders = mutableListOf<String>()
        val availableProviders = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in availableProviders) {
            if (lm.isProviderEnabled(provider)) {
                try {
                    lm.requestLocationUpdates(
                        provider,
                        LOCATION_UPDATE_INTERVAL_MS,
                        LOCATION_MIN_DISTANCE_M,
                        listener
                    )
                    registeredProviders.add(provider)
                } catch (_: Exception) {
                    // Ignore provider registration failure
                }
            }
        }

        onDispose {
            try {
                lm.removeUpdates(listener)
            } catch (_: Exception) {
                // Ignore unregister failure
            }
        }
    }

    return LocationMonitorState(
        status = statusState,
        location = locationState,
        accuracyMeters = accuracyState,
        displayText = textState
    )
}
