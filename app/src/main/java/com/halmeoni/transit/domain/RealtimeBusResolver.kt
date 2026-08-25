package com.halmeoni.transit.domain

import com.halmeoni.transit.data.provider.GyeonggiBusProvider
import com.halmeoni.transit.data.provider.RealtimeBusProvider
import com.halmeoni.transit.data.provider.SeoulBusProvider
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep

class RealtimeBusResolver(
    private val seoulBusProvider: RealtimeBusProvider = SeoulBusProvider(),
    private val gyeonggiBusProvider: RealtimeBusProvider = GyeonggiBusProvider()
) {

    suspend fun resolveBusArrival(step: RouteStep, apiKey: String): RealtimeStatus {
        if (apiKey.isBlank()) {
            return RealtimeStatus.AuthenticationRequired("공공데이터 버스 API 키를 등록해 주세요.")
        }

        val cityCode = step.startCityCode
        val localStationId = step.startLocalStationId ?: ""
        val arsId = step.startArsId ?: ""

        // 1. Check City Code
        if (cityCode == 1000) {
            return seoulBusProvider.getBusArrival(step, apiKey)
        } else if (cityCode == 2000) {
            return gyeonggiBusProvider.getBusArrival(step, apiKey)
        }

        // 2. Heuristic check based on Station ID patterns
        if (arsId.isNotBlank() && (arsId.length == 5 || arsId.matches(Regex("""^\d{5}$""")))) {
            return seoulBusProvider.getBusArrival(step, apiKey)
        }

        if (localStationId.length == 9 && localStationId.startsWith("2")) {
            return gyeonggiBusProvider.getBusArrival(step, apiKey)
        }

        // 3. Fallback attempt for Seoul / Gyeonggi if route IDs are present
        if (step.busLocalRouteId?.startsWith("1") == true || step.busLocalRouteId?.length == 9) {
            val seoulResult = seoulBusProvider.getBusArrival(step, apiKey)
            if (seoulResult is RealtimeStatus.Available) return seoulResult
        }

        if (localStationId.isNotBlank()) {
            val gyeonggiResult = gyeonggiBusProvider.getBusArrival(step, apiKey)
            if (gyeonggiResult is RealtimeStatus.Available) return gyeonggiResult
        }

        return RealtimeStatus.Unsupported("이 지역은 실시간 버스 도착정보가 지원되지 않아요.")
    }
}
