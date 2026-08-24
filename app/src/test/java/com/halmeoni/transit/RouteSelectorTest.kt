package com.halmeoni.transit

import com.halmeoni.transit.domain.RouteSelector
import com.halmeoni.transit.domain.model.TransitRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteSelectorTest {

    private lateinit var routeSelector: RouteSelector

    @Before
    fun setUp() {
        routeSelector = RouteSelector()
    }

    @Test
    fun `calculateScore correctly calculates score based on formula`() {
        val route = createTestRoute(
            transferCount = 1,
            totalWalkDistance = 300,
            totalTime = 25
        )
        // Score = (1 * 1000) + (300 * 2) + (25 * 10) = 1000 + 600 + 250 = 1850
        val score = routeSelector.calculateScore(route)
        assertEquals(1850, score)
    }

    @Test
    fun `selectRoutes excludes routes with transfers over 2`() {
        val validRoute = createTestRoute(id = "r1", transferCount = 2, totalWalkDistance = 500, totalTime = 30)
        val invalidRoute = createTestRoute(id = "r2", transferCount = 3, totalWalkDistance = 200, totalTime = 20)

        val result = routeSelector.selectRoutes(listOf(validRoute, invalidRoute))

        assertNotNull(result.bestRoute)
        assertEquals("r1", result.bestRoute?.id)
        assertTrue(result.alternativeRoutes.isEmpty())
    }

    @Test
    fun `selectRoutes excludes routes with total walk over 1000m`() {
        val validRoute = createTestRoute(id = "r1", transferCount = 1, totalWalkDistance = 900, totalTime = 30)
        val invalidRoute = createTestRoute(id = "r2", transferCount = 1, totalWalkDistance = 1050, totalTime = 20)

        val result = routeSelector.selectRoutes(listOf(validRoute, invalidRoute))

        assertNotNull(result.bestRoute)
        assertEquals("r1", result.bestRoute?.id)
        assertTrue(result.alternativeRoutes.isEmpty())
    }

    @Test
    fun `selectRoutes returns empty when all routes are invalid`() {
        val route1 = createTestRoute(transferCount = 3, totalWalkDistance = 500, totalTime = 20)
        val route2 = createTestRoute(transferCount = 1, totalWalkDistance = 1200, totalTime = 20)

        val result = routeSelector.selectRoutes(listOf(route1, route2))

        assertNull(result.bestRoute)
        assertTrue(result.alternativeRoutes.isEmpty())
    }

    @Test
    fun `selectRoutes prioritizes route with lowest score as bestRoute`() {
        val routeA = createTestRoute(id = "A", transferCount = 1, totalWalkDistance = 400, totalTime = 30) // Score: 2100
        val routeB = createTestRoute(id = "B", transferCount = 0, totalWalkDistance = 500, totalTime = 40) // Score: 1400
        val routeC = createTestRoute(id = "C", transferCount = 2, totalWalkDistance = 200, totalTime = 20) // Score: 2600

        val result = routeSelector.selectRoutes(listOf(routeA, routeB, routeC))

        assertEquals("B", result.bestRoute?.id)
        assertEquals(1400, result.bestRoute?.score)
        assertEquals(2, result.alternativeRoutes.size)
        assertEquals("A", result.alternativeRoutes[0].id)
        assertEquals("C", result.alternativeRoutes[1].id)
    }

    private fun createTestRoute(
        id: String = "test",
        transferCount: Int,
        totalWalkDistance: Int,
        totalTime: Int
    ): TransitRoute {
        return TransitRoute(
            id = id,
            totalTime = totalTime,
            totalWalkDistance = totalWalkDistance,
            totalDistance = 5000,
            transferCount = transferCount,
            payment = 1400,
            firstStartStation = "출발역",
            lastEndStation = "도착역",
            steps = emptyList()
        )
    }
}
