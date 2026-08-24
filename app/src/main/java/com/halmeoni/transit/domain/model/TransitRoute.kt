package com.halmeoni.transit.domain.model

data class TransitRoute(
    val id: String = "",
    val totalTime: Int, // 소요 시간 (분)
    val totalWalkDistance: Int, // 총 도보 거리 (m)
    val totalDistance: Int, // 총 이동 거리 (m)
    val transferCount: Int, // 환승 횟수
    val payment: Int, // 요금 (원)
    val firstStartStation: String,
    val lastEndStation: String,
    val steps: List<RouteStep>,
    val score: Int = 0 // 점수 공식 계산 결과
)
