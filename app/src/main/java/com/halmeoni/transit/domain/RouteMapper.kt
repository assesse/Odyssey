package com.halmeoni.transit.domain

import com.halmeoni.transit.data.api.OdsayPath
import com.halmeoni.transit.data.api.OdsayResponse
import com.halmeoni.transit.data.api.OdsaySubPath
import com.halmeoni.transit.domain.model.RouteStep
import com.halmeoni.transit.domain.model.StepType
import com.halmeoni.transit.domain.model.TransitRoute

class RouteMapper {

    fun mapToDomain(response: OdsayResponse): List<TransitRoute> {
        val paths = response.result?.path ?: return emptyList()
        return paths.mapIndexed { index, path ->
            mapPathToTransitRoute(path, "route_$index")
        }
    }

    private fun mapPathToTransitRoute(path: OdsayPath, id: String): TransitRoute {
        val info = path.info
        val totalTime = info?.totalTime ?: 0
        val totalWalkDistance = info?.totalWalk ?: 0
        val totalDistance = info?.trafficDistance ?: 0
        val busCount = info?.busTransitCount ?: 0
        val subwayCount = info?.subwayTransitCount ?: 0
        val totalTransitUse = busCount + subwayCount
        val transferCount = if (totalTransitUse > 1) totalTransitUse - 1 else 0

        val steps = path.subPath?.map { mapSubPathToRouteStep(it) } ?: emptyList()

        return TransitRoute(
            id = id,
            totalTime = totalTime,
            totalWalkDistance = totalWalkDistance,
            totalDistance = totalDistance,
            transferCount = transferCount,
            payment = info?.payment ?: 0,
            firstStartStation = info?.firstStartStation ?: "",
            lastEndStation = info?.lastEndStation ?: "",
            steps = steps,
            score = 0
        )
    }

    private fun mapSubPathToRouteStep(subPath: OdsaySubPath): RouteStep {
        val trafficType = subPath.trafficType ?: 3
        val type = when (trafficType) {
            1 -> StepType.SUBWAY
            2 -> StepType.BUS
            else -> StepType.WALK
        }

        val firstLane = subPath.lane?.firstOrNull()
        val routeName = when (type) {
            StepType.BUS -> firstLane?.busNo ?: firstLane?.name
            StepType.SUBWAY -> firstLane?.name
            StepType.WALK -> "도보"
        }

        val stepName = when (type) {
            StepType.WALK -> "도보 ${subPath.sectionTime ?: 0}분"
            StepType.BUS -> "${routeName ?: "버스"} 탑승"
            StepType.SUBWAY -> "${routeName ?: "지하철"} 탑승"
        }

        val passStops = subPath.passStopList?.stations?.mapNotNull { it.stationName } ?: emptyList()

        return RouteStep(
            type = type,
            distance = subPath.distance ?: 0,
            sectionTime = subPath.sectionTime ?: 0,
            stepName = stepName,
            startName = subPath.startName ?: "",
            endName = subPath.endName ?: "",
            routeName = routeName,
            stationCount = subPath.stationCount ?: 0,
            passStops = passStops
        )
    }
}
