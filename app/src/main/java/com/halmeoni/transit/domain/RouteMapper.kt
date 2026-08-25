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
        return paths.mapIndexedNotNull { index, path ->
            mapPathToTransitRoute(path, index)
        }
    }

    private fun mapPathToTransitRoute(path: OdsayPath, index: Int): TransitRoute? {
        val info = path.info ?: return null
        val totalTime = info.totalTime ?: 0
        if (totalTime <= 0) return null

        val rawSubPaths = path.subPath ?: return null
        if (rawSubPaths.isEmpty()) return null

        val steps = rawSubPaths.map { mapSubPathToRouteStep(it) }

        // Reject routes with unknown traffic types (prevent pretending unknown vehicle as walk)
        if (steps.any { it.type == StepType.UNKNOWN }) {
            return null
        }

        // Require at least one transit leg (BUS or SUBWAY)
        val hasTransitLeg = steps.any { it.type == StepType.BUS || it.type == StepType.SUBWAY }
        if (!hasTransitLeg) {
            return null
        }

        val totalWalkDistance = info.totalWalk ?: 0
        val totalDistance = info.totalDistance ?: info.trafficDistance ?: 0.0
        val busCount = info.busTransitCount ?: 0
        val subwayCount = info.subwayTransitCount ?: 0
        val totalTransitUse = busCount + subwayCount
        val transferCount = if (totalTransitUse > 1) totalTransitUse - 1 else 0

        // Deterministic stable route ID
        val stableId = "route_${index}_${path.pathType ?: 0}_${totalTime}_${totalWalkDistance}"

        return TransitRoute(
            id = stableId,
            totalTime = totalTime,
            totalWalkDistance = totalWalkDistance,
            totalDistance = totalDistance,
            transferCount = transferCount,
            payment = info.payment ?: 0,
            firstStartStation = info.firstStartStation ?: "",
            lastEndStation = info.lastEndStation ?: "",
            steps = steps
        )
    }

    private fun mapSubPathToRouteStep(subPath: OdsaySubPath): RouteStep {
        val trafficType = subPath.trafficType
        val type = when (trafficType) {
            1 -> StepType.SUBWAY
            2 -> StepType.BUS
            3 -> StepType.WALK
            else -> StepType.UNKNOWN
        }

        val firstLane = subPath.lane?.firstOrNull()
        val routeName = when (type) {
            StepType.BUS -> firstLane?.busNo ?: firstLane?.name
            StepType.SUBWAY -> firstLane?.name
            StepType.WALK -> "도보"
            StepType.UNKNOWN -> null
        }

        val stepName = when (type) {
            StepType.WALK -> "도보 ${subPath.sectionTime ?: 0}분"
            StepType.BUS -> "${routeName ?: "버스"} 탑승"
            StepType.SUBWAY -> "${routeName ?: "지하철"} 탑승"
            StepType.UNKNOWN -> "알 수 없는 이동 수단"
        }

        val passStops = subPath.passStopList?.stations?.mapNotNull { it.stationName } ?: emptyList()

        return RouteStep(
            type = type,
            distance = subPath.distance ?: 0.0,
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
