package com.halmeoni.transit

import com.halmeoni.transit.data.provider.RealtimeBusProvider
import com.halmeoni.transit.data.provider.RealtimeSubwayProvider
import com.halmeoni.transit.data.repository.RealtimeTransitRepository
import com.halmeoni.transit.data.repository.SettingsRepository
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

class RealtimeTransitRepositoryTest {

    private lateinit var testPrefs: TestSharedPreferences
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: RealtimeTransitRepository

    private var busCallCount = 0

    private val fakeBusProvider = object : RealtimeBusProvider {
        override suspend fun getBusArrival(step: RouteStep, apiKey: String): RealtimeStatus {
            busCallCount++
            return RealtimeStatus.Available(
                RealtimeArrival.Bus(
                    firstArrivalMinutes = 3,
                    firstRemainingStops = 2,
                    firstMessage = "약 3분 후 도착"
                )
            )
        }
    }

    private val fakeSubwayProvider = object : RealtimeSubwayProvider {
        override suspend fun getSubwayArrival(step: RouteStep, apiKey: String): RealtimeStatus {
            return RealtimeStatus.Available(
                RealtimeArrival.Subway(
                    arrivalMinutes = 2,
                    arrivalMessage = "약 2분 후 도착",
                    destinationName = "오금행"
                )
            )
        }
    }

    @Before
    fun setUp() {
        busCallCount = 0
        testPrefs = TestSharedPreferences()
        settingsRepository = SettingsRepository(testPrefs)
        settingsRepository.saveBusApiKey("FAKE_BUS_KEY")
        settingsRepository.saveSubwayApiKey("FAKE_SUBWAY_KEY")

        val busResolver = RealtimeBusResolver(
            seoulBusProvider = fakeBusProvider,
            gyeonggiBusProvider = fakeBusProvider
        )

        repository = RealtimeTransitRepository(
            settingsRepository = settingsRepository,
            busResolver = busResolver,
            subwayProvider = fakeSubwayProvider
        )
    }

    @Test
    fun getRealtimeArrival_usesCacheWhenCalledWithinTtl() = runBlocking {
        val step = RouteStep(
            type = StepType.BUS,
            startCityCode = 1000,
            routeName = "661",
            startArsId = "16147"
        )

        // 1st call
        val result1 = repository.getRealtimeArrival(step, forceRefresh = false)
        assertEquals(1, busCallCount)
        assertTrue(result1 is RealtimeStatus.Available)

        // 2nd call (should hit cache)
        val result2 = repository.getRealtimeArrival(step, forceRefresh = false)
        assertEquals(1, busCallCount)
        assertTrue(result2 is RealtimeStatus.Available)

        // Force refresh call (should bypass cache)
        val result3 = repository.getRealtimeArrival(step, forceRefresh = true)
        assertEquals(2, busCallCount)
        assertTrue(result3 is RealtimeStatus.Available)
    }

    @Test
    fun getRealtimeArrival_returnsUnsupportedForWalkStep() = runBlocking {
        val step = RouteStep(type = StepType.WALK, distance = 200.0, sectionTime = 3)
        val result = repository.getRealtimeArrival(step)
        assertTrue(result is RealtimeStatus.Unsupported)
    }
}
