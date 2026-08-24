package com.halmeoni.transit.domain.model

data class Destination(
    val id: String,
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val icon: String,
    val order: Int
)
