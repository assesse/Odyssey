package com.halmeoni.transit.domain.model

sealed interface LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult
    data object PermissionDenied : LocationResult
    data object LocationServiceDisabled : LocationResult
    data object Timeout : LocationResult
    data object Unavailable : LocationResult
    data object Cancelled : LocationResult
}
