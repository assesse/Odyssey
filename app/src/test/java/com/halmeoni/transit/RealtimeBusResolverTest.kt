package com.halmeoni.transit

import com.halmeoni.transit.data.provider.RealtimeBusProvider
import com.halmeoni.transit.domain.RealtimeBusResolver
import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep
import com.halmeoni.transit.domain.model.StepType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealtimeBusResolverTest {

    private lateinit var resolver: RealtimeBusResolver
    private var lastProviderUsed = ""

    private val fakeSeoulProvider = object : RealtimeBusProvider {
        override suspend fun getBusArrival(step: RouteStep, apiKey: String): RealtimeStatus {
            lastProviderUsed = "SEOUL"
            return RealtimeStatus.Available(
                RealtimeArrival.Bus(
                    firstArrivalMinutes = 3,
                    firstRemainingStops = 2,
                    firstMessage = "약 3분 후 도착"
                )
            )
        }
    }

    private val fakeGyeonggiProvider = object : RealtimeBusProvider {
        override suspend fun getBusArrival(step: RouteStep, apiKey: String): RealtimeStatus {
            lastProviderUsed = "GYEONGGI"
            return RealtimeStatus.Available(
                RealtimeArrival.Bus(
                    firstArrivalMinutes = 5,
                    firstRemainingStops = 3,
                    firstMessage = "약 5분 후 도착"
                )
            )
        }
    }

    @Before
    fun setUp() {
        lastProviderUsed = ""
        resolver = RealtimeBusResolver(
            seoulBusProvider = fakeSeoulProvider,
            gyeonggiBusProvider = fakeGyeonggiProvider
        )
    }

    @Test
    fun resolveBusArrival_routesToSeoulProviderForCityCode1000() = runBlocking {
        val step = RouteStep(
            type = StepType.BUS,
            startCityCode = 1000,
            routeName = "661",
            startArsId = "16147"
        )
        val result = resolver.resolveBusArrival(step, "API_KEY")
        assertEquals("SEOUL", lastProviderUsed)
        assertTrue(result is RealtimeStatus.Available)
    }

    @Test
    fun resolveBusArrival_routesToGyeonggiProviderForCityCode2000() = runBlocking {
        val step = RouteStep(
            type = StepType.BUS,
            startCityCode = 2000,
            routeName = "700",
            startLocalStationId = "228000184"
        )
        val result = resolver.resolveBusArrival(step, "API_KEY")
        assertEquals("GYEONGGI", lastProviderUsed)
        assertTrue(result is RealtimeStatus.Available)
    }

    @Test
    fun resolveBusArrival_routesToSeoulBy5DigitArsId() = runBlocking {
        val step = RouteStep(
            type = StepType.BUS,
            startArsId = "01001",
            routeName = "101"
        )
        val result = resolver.resolveBusArrival(step, "API_KEY")
        assertEquals("SEOUL", lastProviderUsed)
        assertTrue(result is RealtimeStatus.Available)
    }

    @Test
    fun resolveBusArrival_requiresApiKey() = runBlocking {
        val step = RouteStep(
            type = StepType.BUS,
            startCityCode = 1000,
            routeName = "661"
        )
        val result = resolver.resolveBusArrival(step, "")
        assertTrue(result is RealtimeStatus.AuthenticationRequired)
    }
}
