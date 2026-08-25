package com.halmeoni.transit.data.provider

import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep

interface RealtimeBusProvider {
    suspend fun getBusArrival(step: RouteStep, apiKey: String): RealtimeStatus
}
