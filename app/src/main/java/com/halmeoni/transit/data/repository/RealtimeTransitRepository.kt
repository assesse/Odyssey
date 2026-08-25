package com.halmeoni.transit.data.repository

import com.halmeoni.transit.data.provider.RealtimeSubwayProvider
import com.halmeoni.transit.data.provider.SeoulSubwayProvider
import com.halmeoni.transit.domain.RealtimeBusResolver
import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep
import com.halmeoni.transit.domain.model.StepType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class RealtimeTransitRepository(
    private val settingsRepository: SettingsRepository,
    private val busResolver: RealtimeBusResolver = RealtimeBusResolver(),
    private val subwayProvider: RealtimeSubwayProvider = SeoulSubwayProvider()
) {

    private data class CachedResult(
        val status: RealtimeStatus,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, CachedResult>()
    private val mutex = Mutex()
    private val cacheTtlMs = 30_000L // 30 seconds TTL

    suspend fun getRealtimeArrival(step: RouteStep, forceRefresh: Boolean = false): RealtimeStatus {
        val cacheKey = buildCacheKey(step)

        if (!forceRefresh) {
            val cached = cache[cacheKey]
            if (cached != null) {
                val age = System.currentTimeMillis() - cached.timestamp
                if (age < cacheTtlMs) {
                    return cached.status
                } else if (cached.status is RealtimeStatus.Available) {
                    // Mark as stale if age exceeded but returned
                    val staleArrival = when (val arr = cached.status.arrival) {
                        is RealtimeArrival.Bus -> arr.copy(isStale = true)
                        is RealtimeArrival.Subway -> arr.copy(isStale = true)
                    }
                    return RealtimeStatus.Stale(staleArrival)
                }
            }
        }

        val result = mutex.withLock {
            when (step.type) {
                StepType.BUS -> {
                    val busApiKey = settingsRepository.getBusApiKey()
                    busResolver.resolveBusArrival(step, busApiKey)
                }
                StepType.SUBWAY -> {
                    val subwayApiKey = settingsRepository.getSubwayApiKey()
                    subwayProvider.getSubwayArrival(step, subwayApiKey)
                }
                StepType.WALK, StepType.UNKNOWN -> {
                    RealtimeStatus.Unsupported("도보는 실시간 정보가 필요하지 않습니다.")
                }
            }
        }

        cache[cacheKey] = CachedResult(result, System.currentTimeMillis())
        return result
    }

    fun clearCache() {
        cache.clear()
    }

    private fun buildCacheKey(step: RouteStep): String {
        return "${step.type}_${step.startStationId}_${step.startLocalStationId}_${step.startArsId}_${step.busId}_${step.busLocalRouteId}_${step.subwayCode}_${step.routeName}_${step.startName}"
    }
}
