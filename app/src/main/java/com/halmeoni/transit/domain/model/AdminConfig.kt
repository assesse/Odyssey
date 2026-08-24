package com.halmeoni.transit.domain.model

data class AdminConfig(
    val pin: String,
    val destinations: List<Destination>,
    val homeLocation: HomeLocation
)
