package com.halmeoni.transit.domain

import com.halmeoni.transit.domain.model.TransitRoute

data class RouteSelectionResult(
    val bestRoute: TransitRoute?,
    val alternativeRoutes: List<TransitRoute>
)

class RouteSelector {

    /**
     * 고령 사용자 맞춤 우선순위 정렬:
     * 1. 환승 횟수가 적은 경로 (0회 환승 > 1회 환승 > 2회 환승 ...)
     * 2. 환승 횟수가 같으면 도보 거리가 짧은 경로
     * 3. 도보 거리도 같으면 총 소요시간이 짧은 경로
     * 4. 모두 같으면 결정적 ID 순서
     *
     * (하드 필터를 두지 않아 정상 경로를 무조건 삭제하지 않음)
     */
    fun selectRoutes(routes: List<TransitRoute>): RouteSelectionResult {
        if (routes.isEmpty()) {
            return RouteSelectionResult(bestRoute = null, alternativeRoutes = emptyList())
        }

        val sortedRoutes = routes.sortedWith(
            compareBy<TransitRoute> { it.transferCount }
                .thenBy { it.totalWalkDistance }
                .thenBy { it.totalTime }
                .thenBy { it.id }
        )

        val bestRoute = sortedRoutes.first()
        val alternativeRoutes = sortedRoutes.drop(1)

        return RouteSelectionResult(bestRoute, alternativeRoutes)
    }
}
