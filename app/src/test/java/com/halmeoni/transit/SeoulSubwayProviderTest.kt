package com.halmeoni.transit

import com.halmeoni.transit.data.provider.SeoulSubwayProvider
import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep
import com.halmeoni.transit.domain.model.StepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeoulSubwayProviderTest {

    private lateinit var provider: SeoulSubwayProvider

    @Before
    fun setUp() {
        provider = SeoulSubwayProvider()
    }

    @Test
    fun parseSubwayArrivalJson_parsesValidSubwayArrival() {
        val json = """
        {
            "errorMessage": {
                "status": 200,
                "code": "INFO-000",
                "message": "정상 처리되었습니다",
                "total": 2
            },
            "realtimeArrivalList": [
                {
                    "subwayId": "1003",
                    "updnLine": "하행",
                    "trainLineNm": "오금행 - 안국방면",
                    "statnNm": "경복궁",
                    "btrainSttus": "일반",
                    "barvlDt": "120",
                    "bstatnNm": "오금",
                    "arvlMsg2": "2분 후 (독립문)",
                    "arvlMsg3": "독립문",
                    "recptnDt": "2026-08-25 19:00:00"
                }
            ]
        }
        """.trimIndent()

        val step = RouteStep(
            type = StepType.SUBWAY,
            startName = "경복궁역",
            endName = "종로3가역",
            subwayCode = 3,
            passStops = listOf("경복궁역", "안국역", "종로3가역")
        )

        val status = provider.parseSubwayArrivalJson(
            json = json,
            targetSubwayId = "1003",
            step = step
        )

        assertTrue(status is RealtimeStatus.Available || status is RealtimeStatus.Stale)
        val arrival = when (status) {
            is RealtimeStatus.Available -> status.arrival as RealtimeArrival.Subway
            is RealtimeStatus.Stale -> status.arrival as RealtimeArrival.Subway
            else -> throw AssertionError("Expected Available or Stale")
        }

        assertEquals(2, arrival.arrivalMinutes)
        assertEquals("오금행", arrival.destinationName)
        assertTrue(arrival.arrivalMessage.contains("2분 후"))
        assertTrue(arrival.currentPositionMsg.contains("독립문"))
    }

    @Test
    fun parseSubwayArrivalJson_filtersByMatchingLineAndDirection() {
        val json = """
        {
            "errorMessage": { "status": 200, "code": "INFO-000", "message": "정상", "total": 2 },
            "realtimeArrivalList": [
                {
                    "subwayId": "1003",
                    "updnLine": "상행",
                    "trainLineNm": "대화행 - 독립문방면",
                    "statnNm": "경복궁",
                    "barvlDt": "60",
                    "bstatnNm": "대화",
                    "arvlMsg2": "1분 후 (안국)"
                },
                {
                    "subwayId": "1003",
                    "updnLine": "하행",
                    "trainLineNm": "오금행 - 안국방면",
                    "statnNm": "경복궁",
                    "barvlDt": "300",
                    "bstatnNm": "오금",
                    "arvlMsg2": "5분 후 (독립문)"
                }
            ]
        }
        """.trimIndent()

        // Looking for heading towards 안국 / 종로3가 (하행 / 오금행)
        val step = RouteStep(
            type = StepType.SUBWAY,
            startName = "경복궁역",
            endName = "종로3가역",
            subwayCode = 3,
            passStops = listOf("경복궁역", "안국역", "종로3가역")
        )

        val status = provider.parseSubwayArrivalJson(
            json = json,
            targetSubwayId = "1003",
            step = step
        )

        assertTrue(status is RealtimeStatus.Available || status is RealtimeStatus.Stale)
        val arrival = when (status) {
            is RealtimeStatus.Available -> status.arrival as RealtimeArrival.Subway
            is RealtimeStatus.Stale -> status.arrival as RealtimeArrival.Subway
            else -> throw AssertionError("Expected Available or Stale")
        }

        assertEquals("오금행", arrival.destinationName)
        assertEquals(5, arrival.arrivalMinutes)
    }

    @Test
    fun normalizeStationName_cleansStationSuffixAndBrackets() {
        assertEquals("경복궁", provider.normalizeStationName("경복궁역"))
        assertEquals("서울역", provider.normalizeStationName("서울역")) // Seoul Station preserves suffix
        assertEquals("총신대입구", provider.normalizeStationName("총신대입구(이수)역"))
    }
}
