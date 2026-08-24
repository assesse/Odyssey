package com.halmeoni.transit.domain

import com.halmeoni.transit.domain.model.TransitRoute

data class RouteSelectionResult(
    val bestRoute: TransitRoute?,
    val alternativeRoutes: List<TransitRoute>
)

class RouteSelector {

    /**
     * 점수 산정 공식:
     * (환승 횟수 * 1000) + (총 도보 거리m * 2) + (총 소요시간분 * 10)
     */
    fun calculateScore(route: TransitRoute): Int {
        return (route.transferCount * 1000) + (route.totalWalkDistance * 2) + (route.totalTime * 10)
    }

    /**
     * 조건:
     * - 환승 2회 초과 제외 (transferCount <= 2만 허용)
     * - 총 도보 1km 초과 제외 (totalWalkDistance <= 1000m 만 허용)
     * - 최적 경로 1개 (가장 낮은 점수) 및 대체 경로 목록 반환
     */
    fun selectRoutes(routes: List<TransitRoute>): RouteSelectionResult {
        val validRoutes = routes.filter { route ->
            route.transferCount <= 2 && route.totalWalkDistance <= 1000
        }

        if (validRoutes.isEmpty()) {
            return RouteSelectionResult(bestRoute = null, alternativeRoutes = emptyList())
        }

        val scoredRoutes = validRoutes.map { route ->
            route.copy(score = calculateScore(route))
        }.sortedWith(
            compareBy<TransitRoute> { it.score }
                .thenBy { it.transferCount }
                .thenBy { it.totalWalkDistance }
                .thenBy { it.totalTime }
        )

        val bestRoute = scoredRoutes.first()
        val alternativeRoutes = scoredRoutes.drop(1)

        return RouteSelectionResult(bestRoute, alternativeRoutes)
    }
}
