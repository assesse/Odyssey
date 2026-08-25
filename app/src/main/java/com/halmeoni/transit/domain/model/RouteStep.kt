package com.halmeoni.transit.domain.model

enum class StepType {
    WALK,
    BUS,
    SUBWAY,
    UNKNOWN
}

data class RouteStep(
    val type: StepType,
    val distance: Double = 0.0,
    val sectionTime: Int = 0,
    val stepName: String = "",
    val startName: String = "",
    val endName: String = "",
    val routeName: String? = null,
    val stationCount: Int = 0,
    val passStops: List<String> = emptyList(),
    val lineType: Int? = null,
    val subwayCode: Int? = null,
    val startStationId: Int? = null,
    val startLocalStationId: String? = null,
    val startArsId: String? = null,
    val startCityCode: Int? = null,
    val startProviderCode: Int? = null,
    val busId: Int? = null,
    val busLocalRouteId: String? = null,
    val subwayWayCode: Int? = null,
    val endStationId: Int? = null,
    val endLocalStationId: String? = null,
    val endArsId: String? = null
)
