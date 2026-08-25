package com.halmeoni.transit

import com.google.gson.GsonBuilder
import com.halmeoni.transit.data.api.OdsayLane
import com.halmeoni.transit.data.api.OdsayPassStopList
import com.halmeoni.transit.data.api.OdsayPath
import com.halmeoni.transit.data.api.OdsayPathInfo
import com.halmeoni.transit.data.api.OdsayResponse
import com.halmeoni.transit.data.api.OdsayResponseDeserializer
import com.halmeoni.transit.data.api.OdsayResult
import com.halmeoni.transit.data.api.OdsayStation
import com.halmeoni.transit.data.api.OdsaySubPath
import com.halmeoni.transit.domain.RouteMapper
import com.halmeoni.transit.domain.model.StepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteMapperTest {

    private lateinit var mapper: RouteMapper
    private val gson = GsonBuilder()
        .registerTypeAdapter(OdsayResponse::class.java, OdsayResponseDeserializer())
        .create()

    @Before
    fun setUp() {
        mapper = RouteMapper()
    }

    @Test
    fun mapToDomain_parsesPrecisionDoubleDistancesAndSteps() {
        val json = """
        {
            "result": {
                "searchType": 0,
                "outTrafficCheck": 0,
                "busCount": 1,
                "subwayCount": 0,
                "subwayBusCount": 0,
                "pointDistance": 3500.25,
                "startRadius": 700,
                "endRadius": 700,
                "path": [
                    {
                        "pathType": 2,
                        "info": {
                            "trafficDistance": 3520.55,
                            "totalDistance": 4120.85,
                            "totalWalk": 600,
                            "totalTime": 32,
                            "payment": 1400,
                            "busTransitCount": 1,
                            "subwayTransitCount": 0,
                            "firstStartStation": "시청앞",
                            "lastEndStation": "종로2가",
                            "totalStationCount": 5
                        },
                        "subPath": [
                            {
                                "trafficType": 3,
                                "distance": 235.45,
                                "sectionTime": 4
                            },
                            {
                                "trafficType": 2,
                                "distance": 3520.55,
                                "sectionTime": 24,
                                "stationCount": 5,
                                "startName": "시청앞",
                                "endName": "종로2가",
                                "startID": 11111,
                                "startLocalStationID": "100000001",
                                "startArsID": "01001",
                                "startStationCityCode": 1000,
                                "lane": [
                                    { "name": "101번", "busNo": "101", "busID": 1001, "busLocalBlID": "100100001", "type": 11 }
                                ],
                                "passStopList": {
                                    "stations": [
                                        { "index": 1, "stationName": "시청앞", "stationID": 11111 },
                                        { "index": 2, "stationName": "을지로입구", "stationID": 11112 },
                                        { "index": 3, "stationName": "종로2가", "stationID": 11113 }
                                    ]
                                }
                            },
                            {
                                "trafficType": 3,
                                "distance": 364.85,
                                "sectionTime": 4
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OdsayResponse::class.java)
        assertEquals(0, response.result?.outTrafficCheck)

        val routes = mapper.mapToDomain(response)
        assertEquals(1, routes.size)

        val route = routes[0]
        assertEquals(32, route.totalTime)
        assertEquals(600, route.totalWalkDistance)
        assertEquals(4120.85, route.totalDistance, 0.001)
        assertEquals(0, route.transferCount)
        assertEquals(3, route.steps.size)

        // Step 1: Walk
        assertEquals(StepType.WALK, route.steps[0].type)
        assertEquals(235.45, route.steps[0].distance, 0.001)
        assertEquals(4, route.steps[0].sectionTime)

        // Step 2: Bus (Preserves Realtime Metadata)
        val busStep = route.steps[1]
        assertEquals(StepType.BUS, busStep.type)
        assertEquals("101", busStep.routeName)
        assertEquals("시청앞", busStep.startName)
        assertEquals("종로2가", busStep.endName)
        assertEquals(3520.55, busStep.distance, 0.001)
        assertEquals(3, busStep.passStops.size)
        assertEquals(11, busStep.lineType)
        assertEquals(11111, busStep.startStationId)
        assertEquals("100000001", busStep.startLocalStationId)
        assertEquals("01001", busStep.startArsId)
        assertEquals(1000, busStep.startCityCode)
        assertEquals(1001, busStep.busId)
        assertEquals("100100001", busStep.busLocalRouteId)

        // Step 3: Walk
        assertEquals(StepType.WALK, route.steps[2].type)
        assertEquals(364.85, route.steps[2].distance, 0.001)
    }

    @Test
    fun mapToDomain_preservesSubwayMetadataForRealtime() {
        val path = OdsayPath(
            pathType = 1,
            info = OdsayPathInfo(totalTime = 20, totalWalk = 100, trafficDistance = 5000.0),
            subPath = listOf(
                OdsaySubPath(
                    trafficType = 1,
                    distance = 5000.0,
                    sectionTime = 15,
                    startName = "경복궁역",
                    endName = "종로3가역",
                    startID = 327,
                    endID = 329,
                    wayCode = 2,
                    lane = listOf(OdsayLane(name = "수도권 3호선", subwayCode = 3)),
                    passStopList = OdsayPassStopList(
                        stations = listOf(
                            OdsayStation(index = 1, stationName = "경복궁역"),
                            OdsayStation(index = 2, stationName = "안국역"),
                            OdsayStation(index = 3, stationName = "종로3가역")
                        )
                    )
                )
            )
        )
        val response = OdsayResponse(result = OdsayResult(path = listOf(path)))
        val routes = mapper.mapToDomain(response)

        assertEquals(1, routes.size)
        val step = routes[0].steps[0]
        assertEquals(StepType.SUBWAY, step.type)
        assertEquals(3, step.subwayCode)
        assertEquals(327, step.startStationId)
        assertEquals(329, step.endStationId)
        assertEquals(2, step.subwayWayCode)
        assertEquals("안국역", step.passStops[1])
    }

    @Test
    fun mapToDomain_rejectsRoutesWithUnknownTrafficType_doesNotPretendAsWalk() {
        val path = OdsayPath(
            pathType = 2,
            info = OdsayPathInfo(totalTime = 30, totalWalk = 200, trafficDistance = 2000.0, busTransitCount = 1),
            subPath = listOf(
                OdsaySubPath(trafficType = 999, distance = 500.0, sectionTime = 10), // Unknown vehicle
                OdsaySubPath(trafficType = 2, distance = 1500.0, sectionTime = 20, startName = "정류장A", endName = "정류장B",
                    lane = listOf(OdsayLane(busNo = "701"))
                )
            )
        )
        val response = OdsayResponse(result = OdsayResult(path = listOf(path)))

        val routes = mapper.mapToDomain(response)
        assertTrue(routes.isEmpty())
    }

    @Test
    fun mapToDomain_rejectsPureWalkingRouteWithoutTransitLegs() {
        val pureWalkPath = OdsayPath(
            pathType = 3,
            info = OdsayPathInfo(totalTime = 15, totalWalk = 800, trafficDistance = 0.0),
            subPath = listOf(
                OdsaySubPath(trafficType = 3, distance = 800.0, sectionTime = 15)
            )
        )
        val response = OdsayResponse(result = OdsayResult(path = listOf(pureWalkPath)))

        val routes = mapper.mapToDomain(response)
        assertTrue(routes.isEmpty())
    }
}
