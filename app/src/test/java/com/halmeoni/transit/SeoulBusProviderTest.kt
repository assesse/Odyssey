package com.halmeoni.transit

import com.halmeoni.transit.data.provider.SeoulBusProvider
import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeoulBusProviderTest {

    private lateinit var provider: SeoulBusProvider

    @Before
    fun setUp() {
        provider = SeoulBusProvider()
    }

    @Test
    fun parseSeoulBusXml_parsesValidArrivalInformation() {
        val xml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <ServiceResult>
            <msgHeader>
                <headerCd>0</headerCd>
                <headerMsg>정상적으로 처리되었습니다.</headerMsg>
                <itemCount>0</itemCount>
            </msgHeader>
            <msgBody>
                <itemList>
                    <arsId>01001</arsId>
                    <busRouteId>100100001</busRouteId>
                    <rtNm>101</rtNm>
                    <stNm>시청앞</stNm>
                    <arrmsg1>3분[2번째 전]</arrmsg1>
                    <arrmsg2>11분[7번째 전]</arrmsg2>
                    <traTime1>180</traTime1>
                    <traTime2>660</traTime2>
                </itemList>
            </msgBody>
        </ServiceResult>
        """.trimIndent()

        val status = provider.parseSeoulBusXml(
            xml = xml,
            targetBusNo = "101",
            targetArsId = "01001",
            targetStationName = "시청앞"
        )

        assertTrue(status is RealtimeStatus.Available)
        val arrival = (status as RealtimeStatus.Available).arrival as RealtimeArrival.Bus

        assertEquals(3, arrival.firstArrivalMinutes)
        assertEquals(2, arrival.firstRemainingStops)
        assertTrue(arrival.firstMessage.contains("3분 후 도착"))
        assertTrue(arrival.firstMessage.contains("2정거장 전"))

        assertEquals(11, arrival.secondArrivalMinutes)
        assertEquals(7, arrival.secondRemainingStops)
        assertTrue(arrival.secondMessage?.contains("11분 후") == true)
    }

    @Test
    fun parseSeoulBusXml_handlesArrivingSoonMessage() {
        val xml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <ServiceResult>
            <msgHeader><headerCd>0</headerCd><headerMsg>정상</headerMsg></msgHeader>
            <msgBody>
                <itemList>
                    <arsId>16147</arsId>
                    <rtNm>661</rtNm>
                    <stNm>신월5동주민센터</stNm>
                    <arrmsg1>곧 도착</arrmsg1>
                    <arrmsg2>8분[5번째 전]</arrmsg2>
                </itemList>
            </msgBody>
        </ServiceResult>
        """.trimIndent()

        val status = provider.parseSeoulBusXml(
            xml = xml,
            targetBusNo = "661",
            targetArsId = "16147",
            targetStationName = "신월5동"
        )

        assertTrue(status is RealtimeStatus.Available)
        val arrival = (status as RealtimeStatus.Available).arrival as RealtimeArrival.Bus
        assertTrue(arrival.firstMessage.contains("곧 도착"))
    }

    @Test
    fun parseSeoulBusXml_returnsNoDataOnServiceEnd() {
        val xml = """
        <ServiceResult>
            <msgHeader><headerCd>0</headerCd><headerMsg>정상</headerMsg></msgHeader>
            <msgBody>
                <itemList>
                    <arsId>01001</arsId>
                    <rtNm>101</rtNm>
                    <stNm>시청앞</stNm>
                    <arrmsg1>운행종료</arrmsg1>
                    <arrmsg2>운행종료</arrmsg2>
                </itemList>
            </msgBody>
        </ServiceResult>
        """.trimIndent()

        val status = provider.parseSeoulBusXml(
            xml = xml,
            targetBusNo = "101",
            targetArsId = "01001",
            targetStationName = "시청앞"
        )

        assertTrue(status is RealtimeStatus.NoData)
    }

    @Test
    fun parseSeoulBusXml_returnsAuthErrorOnInvalidKey() {
        val xml = """
        <ServiceResult>
            <msgHeader>
                <headerCd>30</headerCd>
                <headerMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</headerMsg>
            </msgHeader>
        </ServiceResult>
        """.trimIndent()

        val status = provider.parseSeoulBusXml(
            xml = xml,
            targetBusNo = "101",
            targetArsId = "01001",
            targetStationName = "시청앞"
        )

        assertTrue(status is RealtimeStatus.AuthenticationRequired)
    }
}
