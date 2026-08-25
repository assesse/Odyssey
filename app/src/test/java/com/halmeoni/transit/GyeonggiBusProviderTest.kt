package com.halmeoni.transit

import com.halmeoni.transit.data.provider.GyeonggiBusProvider
import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GyeonggiBusProviderTest {

    private lateinit var provider: GyeonggiBusProvider

    @Before
    fun setUp() {
        provider = GyeonggiBusProvider()
    }

    @Test
    fun parseGyeonggiBusXml_parsesValidBusArrivalList() {
        val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <response>
            <msgHeader>
                <resultCode>0</resultCode>
                <resultMessage>정상적으로 처리되었습니다.</resultMessage>
            </msgHeader>
            <msgBody>
                <busArrivalList>
                    <stationId>228000184</stationId>
                    <routeId>200000085</routeId>
                    <routeName>700</routeName>
                    <predictTime1>4</predictTime1>
                    <locationNo1>3</locationNo1>
                    <predictTime2>15</predictTime2>
                    <locationNo2>9</locationNo2>
                </busArrivalList>
            </msgBody>
        </response>
        """.trimIndent()

        val status = provider.parseGyeonggiBusXml(
            xml = xml,
            targetBusNo = "700",
            targetRouteId = "200000085"
        )

        assertTrue(status is RealtimeStatus.Available)
        val arrival = (status as RealtimeStatus.Available).arrival as RealtimeArrival.Bus

        assertEquals(4, arrival.firstArrivalMinutes)
        assertEquals(3, arrival.firstRemainingStops)
        assertTrue(arrival.firstMessage.contains("4분 후"))
        assertTrue(arrival.firstMessage.contains("3정거장 전"))

        assertEquals(15, arrival.secondArrivalMinutes)
        assertEquals(9, arrival.secondRemainingStops)
        assertTrue(arrival.secondMessage?.contains("15분 후") == true)
    }

    @Test
    fun parseGyeonggiBusXml_handlesAuthError() {
        val xml = """
        <response>
            <msgHeader>
                <resultCode>30</resultCode>
                <resultMessage>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</resultMessage>
            </msgHeader>
        </response>
        """.trimIndent()

        val status = provider.parseGyeonggiBusXml(
            xml = xml,
            targetBusNo = "700",
            targetRouteId = "200000085"
        )

        assertTrue(status is RealtimeStatus.AuthenticationRequired)
    }
}
