package com.halmeoni.transit.data.provider

import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep

interface RealtimeSubwayProvider {
    suspend fun getSubwayArrival(step: RouteStep, apiKey: String): RealtimeStatus
}
