package com.halmeoni.transit

import com.halmeoni.transit.data.api.OdsayLane
import com.halmeoni.transit.data.api.OdsayPassStopList
import com.halmeoni.transit.data.api.OdsayPath
import com.halmeoni.transit.data.api.OdsayPathInfo
import com.halmeoni.transit.data.api.OdsayResponse
import com.halmeoni.transit.data.api.OdsayResult
import com.halmeoni.transit.data.api.OdsayStation
import com.halmeoni.transit.data.api.OdsaySubPath
import com.halmeoni.transit.domain.RouteMapper
import com.halmeoni.transit.domain.model.StepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteMapperTest {

    private lateinit var routeMapper: RouteMapper

    @Before
    fun setUp() {
        routeMapper = RouteMapper()
    }

    @Test
    fun `mapToDomain maps empty response to empty list`() {
        val response = OdsayResponse(result = null)
        val result = routeMapper.mapToDomain(response)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mapToDomain correctly maps OdsayResponse to TransitRoute list`() {
        val odsayResponse = OdsayResponse(
            result = OdsayResult(
                searchType = 0,
                path = listOf(
                    OdsayPath(
                        pathType = 2,
                        info = OdsayPathInfo(
                            totalTime = 35,
                            totalWalk = 450,
                            trafficDistance = 8000,
                            payment = 1400,
                            busTransitCount = 2,
                            subwayTransitCount = 0,
                            firstStartStation = "시청역",
                            lastEndStation = "서울대병원"
                        ),
                        subPath = listOf(
                            OdsaySubPath(
                                trafficType = 3,
                                distance = 200,
                                sectionTime = 3,
                                startName = "집",
                                endName = "시청역 정류장"
                            ),
                            OdsaySubPath(
                                trafficType = 2,
                                distance = 7800,
                                sectionTime = 30,
                                stationCount = 10,
                                startName = "시청역 정류장",
                                endName = "서울대병원 정류장",
                                lane = listOf(OdsayLane(busNo = "720")),
                                passStopList = OdsayPassStopList(
                                    stations = listOf(
                                        OdsayStation(stationName = "시청역 정류장"),
                                        OdsayStation(stationName = "종로3가"),
                                        OdsayStation(stationName = "서울대병원 정류장")
                                    )
                                )
                            ),
                            OdsaySubPath(
                                trafficType = 3,
                                distance = 250,
                                sectionTime = 2,
                                startName = "서울대병원 정류장",
                                endName = "서울대병원"
                            )
                        )
                    )
                )
            )
        )

        val routes = routeMapper.mapToDomain(odsayResponse)

        assertEquals(1, routes.size)
        val route = routes.first()

        assertEquals(35, route.totalTime)
        assertEquals(450, route.totalWalkDistance)
        assertEquals(8000, route.totalDistance)
        assertEquals(1, route.transferCount)
        assertEquals(1400, route.payment)
        assertEquals("시청역", route.firstStartStation)
        assertEquals("서울대병원", route.lastEndStation)

        assertEquals(3, route.steps.size)
        assertEquals(StepType.WALK, route.steps[0].type)
        assertEquals(StepType.BUS, route.steps[1].type)
        assertEquals("720", route.steps[1].routeName)
        assertEquals(10, route.steps[1].stationCount)
        assertEquals(3, route.steps[1].passStops.size)
        assertEquals("종로3가", route.steps[1].passStops[1])
        assertEquals(StepType.WALK, route.steps[2].type)
    }
}
