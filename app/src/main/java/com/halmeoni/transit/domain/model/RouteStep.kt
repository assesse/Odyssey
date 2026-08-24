package com.halmeoni.transit.domain.model

enum class StepType {
    WALK, BUS, SUBWAY
}

data class RouteStep(
    val type: StepType,
    val distance: Int, // 미터 단위
    val sectionTime: Int, // 분 단위
    val stepName: String, // 예: "도보 5분", "720번 버스", "3호선"
    val startName: String,
    val endName: String,
    val routeName: String? = null,
    val stationCount: Int = 0,
    val passStops: List<String> = emptyList()
)
