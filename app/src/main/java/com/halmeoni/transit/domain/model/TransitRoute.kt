package com.halmeoni.transit.domain.model

data class TransitRoute(
    val id: String,
    val totalTime: Int,
    val totalWalkDistance: Int,
    val totalDistance: Double,
    val transferCount: Int,
    val payment: Int,
    val firstStartStation: String,
    val lastEndStation: String,
    val steps: List<RouteStep>
)
