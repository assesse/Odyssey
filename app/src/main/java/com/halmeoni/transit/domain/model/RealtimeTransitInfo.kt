package com.halmeoni.transit.domain.model

sealed class RealtimeStatus {
    object NotRequested : RealtimeStatus()
    object Loading : RealtimeStatus()
    data class Available(val arrival: RealtimeArrival) : RealtimeStatus()
    data class Stale(val arrival: RealtimeArrival) : RealtimeStatus()
    data class NoData(val message: String = "현재 도착 정보가 없어요.") : RealtimeStatus()
    data class Unsupported(val message: String = "이 구간은 실시간 정보가 지원되지 않아요.") : RealtimeStatus()
    data class AuthenticationRequired(val message: String = "실시간 API 키 설정이 필요해요.") : RealtimeStatus()
    data class NetworkError(val message: String = "실시간 정보만 확인하지 못했어요.") : RealtimeStatus()
}

sealed class RealtimeArrival {
    abstract val isStale: Boolean
    abstract val fetchedAt: Long

    data class Bus(
        val firstArrivalMinutes: Int?,
        val firstRemainingStops: Int?,
        val firstMessage: String,
        val secondArrivalMinutes: Int? = null,
        val secondRemainingStops: Int? = null,
        val secondMessage: String? = null,
        override val isStale: Boolean = false,
        override val fetchedAt: Long = System.currentTimeMillis()
    ) : RealtimeArrival()

    data class Subway(
        val arrivalMinutes: Int?,
        val arrivalMessage: String,
        val destinationName: String = "",
        val nextStationDirection: String = "",
        val currentPositionMsg: String = "",
        override val isStale: Boolean = false,
        override val fetchedAt: Long = System.currentTimeMillis()
    ) : RealtimeArrival()
}
