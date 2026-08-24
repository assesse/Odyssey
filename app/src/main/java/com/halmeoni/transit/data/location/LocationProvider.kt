package com.halmeoni.transit.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val isHomeSubstituted: Boolean = false,
    val isFallback: Boolean = false
)

class LocationProvider(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(
        homeLat: Double? = null,
        homeLng: Double? = null,
        timeoutMs: Long = 10_000L
    ): UserLocation {
        val result = withTimeoutOrNull(timeoutMs) {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                val locationTask = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                val location: Location? = locationTask.await()
                
                if (location != null) {
                    if (homeLat != null && homeLng != null) {
                        val distanceResults = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude,
                            location.longitude,
                            homeLat,
                            homeLng,
                            distanceResults
                        )
                        val distanceToHomeInMeters = distanceResults[0]
                        if (distanceToHomeInMeters <= 300.0) {
                            return@withTimeoutOrNull UserLocation(
                                latitude = homeLat,
                                longitude = homeLng,
                                isHomeSubstituted = true,
                                isFallback = false
                            )
                        }
                    }
                    UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isHomeSubstituted = false,
                        isFallback = false
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        return result ?: if (homeLat != null && homeLng != null) {
            UserLocation(
                latitude = homeLat,
                longitude = homeLng,
                isHomeSubstituted = true,
                isFallback = true
            )
        } else {
            UserLocation(
                latitude = 37.5665,
                longitude = 126.9780,
                isHomeSubstituted = false,
                isFallback = true
            )
        }
    }
}
