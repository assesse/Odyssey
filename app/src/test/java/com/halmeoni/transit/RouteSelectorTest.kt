package com.halmeoni.transit

import com.halmeoni.transit.domain.RouteSelector
import com.halmeoni.transit.domain.model.TransitRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RouteSelectorTest {

    private lateinit var routeSelector: RouteSelector

    @Before
    fun setUp() {
        routeSelector = RouteSelector()
    }

    private fun createRoute(
        id: String,
        transferCount: Int,
        totalWalkDistance: Int,
        totalTime: Int,
        totalDistance: Double = 5000.0
    ): TransitRoute {
        return TransitRoute(
            id = id,
            totalTime = totalTime,
            totalWalkDistance = totalWalkDistance,
            totalDistance = totalDistance,
            transferCount = transferCount,
            payment = 1400,
            firstStartStation = "출발역",
            lastEndStation = "도착역",
            steps = emptyList()
        )
    }

    @Test
    fun selectRoutes_emptyList_returnsNullBestRoute() {
        val result = routeSelector.selectRoutes(emptyList())
        assertNull(result.bestRoute)
        assertEquals(0, result.alternativeRoutes.size)
    }

    @Test
    fun selectRoutes_prioritizesLeastTransfersOverShorterTime() {
        // A: 0 transfers, 800m walk, 40 min
        val routeA = createRoute("route_A", transferCount = 0, totalWalkDistance = 800, totalTime = 40)
        // B: 1 transfer, 50m walk, 15 min
        val routeB = createRoute("route_B", transferCount = 1, totalWalkDistance = 50, totalTime = 15)

        val result = routeSelector.selectRoutes(listOf(routeB, routeA))

        assertNotNull(result.bestRoute)
        assertEquals("route_A", result.bestRoute?.id)
        assertEquals(1, result.alternativeRoutes.size)
        assertEquals("route_B", result.alternativeRoutes[0].id)
    }

    @Test
    fun selectRoutes_whenTransfersEqual_prioritizesLeastWalking() {
        // A: 1 transfer, 200m walk, 35 min
        val routeA = createRoute("route_A", transferCount = 1, totalWalkDistance = 200, totalTime = 35)
        // B: 1 transfer, 400m walk, 20 min
        val routeB = createRoute("route_B", transferCount = 1, totalWalkDistance = 400, totalTime = 20)

        val result = routeSelector.selectRoutes(listOf(routeB, routeA))

        assertNotNull(result.bestRoute)
        assertEquals("route_A", result.bestRoute?.id)
    }

    @Test
    fun selectRoutes_whenTransfersAndWalkingEqual_prioritizesShortestTime() {
        // A: 1 transfer, 200m walk, 35 min
        val routeA = createRoute("route_A", transferCount = 1, totalWalkDistance = 200, totalTime = 35)
        // B: 1 transfer, 200m walk, 25 min
        val routeB = createRoute("route_B", transferCount = 1, totalWalkDistance = 200, totalTime = 25)

        val result = routeSelector.selectRoutes(listOf(routeA, routeB))

        assertNotNull(result.bestRoute)
        assertEquals("route_B", result.bestRoute?.id)
    }

    @Test
    fun selectRoutes_doesNotDiscardRouteWithHighTransfersOrLongWalk() {
        // Route with 3 transfers and 1200m walk (previously discarded by hard filters)
        val highTransferRoute = createRoute("high_transfer", transferCount = 3, totalWalkDistance = 1200, totalTime = 60)

        val result = routeSelector.selectRoutes(listOf(highTransferRoute))

        assertNotNull(result.bestRoute)
        assertEquals("high_transfer", result.bestRoute?.id)
    }
}
