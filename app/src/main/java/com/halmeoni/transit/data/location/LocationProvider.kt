package com.halmeoni.transit.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.halmeoni.transit.domain.model.LocationResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

interface LocationProvider {
    fun hasLocationPermission(): Boolean
    fun isLocationServiceEnabled(): Boolean
    suspend fun getCurrentLocation(timeoutMs: Long = 10_000L): LocationResult
}

class FusedLocationProvider(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationProvider {

    override fun hasLocationPermission(): Boolean {
        val finePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return finePermission || coarsePermission
    }

    override fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        val isGpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            false
        }
        val isNetworkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
        return isGpsEnabled || isNetworkEnabled
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(timeoutMs: Long): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.PermissionDenied
        }

        if (!isLocationServiceEnabled()) {
            return LocationResult.LocationServiceDisabled
        }

        val cancellationTokenSource = CancellationTokenSource()
        return try {
            val result = withTimeoutOrNull(timeoutMs) {
                try {
                    val locationTask = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    )
                    val location: Location? = locationTask.await()
                    if (location != null) {
                        LocationResult.Success(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    } else {
                        LocationResult.Unavailable
                    }
                } catch (e: Exception) {
                    LocationResult.Unavailable
                }
            }
            result ?: LocationResult.Timeout
        } catch (e: Exception) {
            LocationResult.Unavailable
        }
    }
}
