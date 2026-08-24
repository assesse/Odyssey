package com.halmeoni.transit.domain.model

sealed interface RouteRequest {
    data class ToDestination(val destinationId: String) : RouteRequest
    data object GoHome : RouteRequest
}
