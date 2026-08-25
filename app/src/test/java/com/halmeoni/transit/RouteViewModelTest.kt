package com.halmeoni.transit

import com.halmeoni.transit.data.location.LocationProvider
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.RouteRepository
import com.halmeoni.transit.data.repository.RouteRepositoryError
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.data.repository.TransitRouteResult
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.RouteSelector
import com.halmeoni.transit.domain.model.Destination
import com.halmeoni.transit.domain.model.HomeLocation
import com.halmeoni.transit.domain.model.LocationResult
import com.halmeoni.transit.domain.model.RouteRequest
import com.halmeoni.transit.domain.model.TransitRoute
import com.halmeoni.transit.ui.route.RouteErrorType
import com.halmeoni.transit.ui.route.RouteUiState
import com.halmeoni.transit.ui.route.RouteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRouteRepository : RouteRepository {
    var lastStartLat: Double? = null
    var lastStartLng: Double? = null
    var lastEndLat: Double? = null
    var lastEndLng: Double? = null
    var callCount = 0

    var returnResult: TransitRouteResult = TransitRouteResult.Success(emptyList())
    var delayMs: Long = 0L

    override suspend fun getTransitRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): TransitRouteResult {
        callCount++
        lastStartLat = startLat
        lastStartLng = startLng
        lastEndLat = endLat
        lastEndLng = endLng

        if (delayMs > 0) {
            delay(delayMs)
        }
        return returnResult
    }
}

class FakeTestLocationProvider : LocationProvider {
    var simulatedResult: LocationResult = LocationResult.Success(37.5500, 126.9500)
    var permissionGranted: Boolean = true
    var serviceEnabled: Boolean = true

    override fun hasLocationPermission(): Boolean = permissionGranted
    override fun isLocationServiceEnabled(): Boolean = serviceEnabled

    override suspend fun getCurrentLocation(timeoutMs: Long): LocationResult {
        if (!permissionGranted) return LocationResult.PermissionDenied
        if (!serviceEnabled) return LocationResult.LocationServiceDisabled
        return simulatedResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RouteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRouteRepo: FakeRouteRepository
    private lateinit var testPrefs: TestSharedPreferences
    private lateinit var destRepo: DestinationRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var apiUsageTracker: ApiUsageTracker
    private lateinit var fakeLocationProvider: FakeTestLocationProvider

    private val testHome = HomeLocation(latitude = 37.5000, longitude = 127.0000, address = "서울시 강남구")
    private val testHospital = Destination(
        id = "dest_hospital",
        name = "서울대학교병원",
        displayName = "병원",
        latitude = 37.5796,
        longitude = 126.9990,
        icon = "hospital",
        order = 1
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeRouteRepo = FakeRouteRepository()
        testPrefs = TestSharedPreferences()

        destRepo = DestinationRepository(testPrefs)
        settingsRepo = SettingsRepository(testPrefs)
        settingsRepo.saveApiKey("TEST_ODSAY_API_KEY")
        apiUsageTracker = ApiUsageTracker(testPrefs)
        fakeLocationProvider = FakeTestLocationProvider()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RouteViewModel {
        return RouteViewModel(
            routeRepository = fakeRouteRepo,
            locationProvider = fakeLocationProvider,
            destinationRepository = destRepo,
            settingsRepository = settingsRepo,
            apiUsageTracker = apiUsageTracker,
            routeSelector = RouteSelector()
        )
    }

    @Test
    fun toDestination_routesFromHomeToDestinationCoordinates() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        destRepo.saveDestination(testHospital)

        val sampleRoute = TransitRoute(
            id = "r1",
            totalTime = 30,
            totalWalkDistance = 200,
            totalDistance = 5000.0,
            transferCount = 0,
            payment = 1400,
            firstStartStation = "강남역",
            lastEndStation = "혜화역",
            steps = emptyList()
        )
        fakeRouteRepo.returnResult = TransitRouteResult.Success(listOf(sampleRoute))

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        advanceUntilIdle()

        // Verify Direction: Start is Home, End is Destination
        assertEquals(testHome.latitude, fakeRouteRepo.lastStartLat)
        assertEquals(testHome.longitude, fakeRouteRepo.lastStartLng)
        assertEquals(testHospital.latitude, fakeRouteRepo.lastEndLat)
        assertEquals(testHospital.longitude, fakeRouteRepo.lastEndLng)

        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Success)
        assertEquals("병원", (state as RouteUiState.Success).destinationTitle)
    }

    @Test
    fun goHome_routesFromCurrentLocationToHomeCoordinates() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        fakeLocationProvider.simulatedResult = LocationResult.Success(latitude = 37.5500, longitude = 126.9500)

        val sampleRoute = TransitRoute(
            id = "r2",
            totalTime = 25,
            totalWalkDistance = 150,
            totalDistance = 4000.0,
            transferCount = 0,
            payment = 1400,
            firstStartStation = "마포역",
            lastEndStation = "강남역",
            steps = emptyList()
        )
        fakeRouteRepo.returnResult = TransitRouteResult.Success(listOf(sampleRoute))

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.GoHome)
        advanceUntilIdle()

        // Verify Direction: Start is GPS Location, End is Home
        assertEquals(37.5500, fakeRouteRepo.lastStartLat)
        assertEquals(126.9500, fakeRouteRepo.lastStartLng)
        assertEquals(testHome.latitude, fakeRouteRepo.lastEndLat)
        assertEquals(testHome.longitude, fakeRouteRepo.lastEndLng)

        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Success)
        assertEquals("우리 집", (state as RouteUiState.Success).destinationTitle)
    }

    @Test
    fun unconfiguredHomeLocation_doesNotCallApi_andReturnsConfigurationError() = runTest {
        // Home is not configured
        destRepo.saveDestination(testHospital)

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        advanceUntilIdle()

        assertEquals(0, fakeRouteRepo.callCount)
        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Error)
        assertEquals(RouteErrorType.CONFIGURATION_REQUIRED, (state as RouteUiState.Error).errorType)
    }

    @Test
    fun nonExistentDestinationId_doesNotFallbackToHospital_andReturnsError() = runTest {
        settingsRepo.saveHomeLocation(testHome)

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination("unknown_id_999"))
        advanceUntilIdle()

        assertEquals(0, fakeRouteRepo.callCount)
        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Error)
        assertEquals(RouteErrorType.CONFIGURATION_REQUIRED, (state as RouteUiState.Error).errorType)
    }

    @Test
    fun apiKeyNotConfigured_doesNotReturnSampleRoute_andReturnsConfigurationError() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        destRepo.saveDestination(testHospital)
        settingsRepo.saveApiKey("") // Explicitly unconfigured
        fakeRouteRepo.returnResult = TransitRouteResult.Failure(RouteRepositoryError.ApiKeyNotConfigured)

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Error)
        assertEquals(RouteErrorType.CONFIGURATION_REQUIRED, (state as RouteUiState.Error).errorType)
    }

    @Test
    fun locationFailures_doNotFallbackToSeoulCityHallOrHome() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        val viewModel = createViewModel()

        // 1. Permission Denied
        fakeLocationProvider.permissionGranted = false
        viewModel.loadRoute(RouteRequest.GoHome)
        advanceUntilIdle()
        assertEquals(0, fakeRouteRepo.callCount)
        var state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Error)
        assertEquals(RouteErrorType.PERMISSION_REQUIRED, (state as RouteUiState.Error).errorType)

        // 2. Location Service Disabled
        fakeLocationProvider.permissionGranted = true
        fakeLocationProvider.serviceEnabled = false
        viewModel.loadRoute(RouteRequest.GoHome)
        advanceUntilIdle()
        assertEquals(0, fakeRouteRepo.callCount)
        state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Error)
        assertEquals(RouteErrorType.LOCATION_SERVICE_DISABLED, (state as RouteUiState.Error).errorType)

        // 3. Timeout
        fakeLocationProvider.serviceEnabled = true
        fakeLocationProvider.simulatedResult = LocationResult.Timeout
        viewModel.loadRoute(RouteRequest.GoHome)
        advanceUntilIdle()
        assertEquals(0, fakeRouteRepo.callCount)
        state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Error)
        assertEquals(RouteErrorType.LOCATION_UNAVAILABLE, (state as RouteUiState.Error).errorType)
    }

    @Test
    fun rapidConsecutiveRequests_cancelPreviousJob_andDisplayLatestDestination() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        val destMarket = Destination("dest_market", "경동시장", "시장", 37.5804, 127.0385, "market", 2)
        destRepo.saveDestination(testHospital)
        destRepo.saveDestination(destMarket)

        fakeRouteRepo.delayMs = 100
        val sampleRoute = TransitRoute(
            id = "r3",
            totalTime = 20,
            totalWalkDistance = 100,
            totalDistance = 3000.0,
            transferCount = 0,
            payment = 1400,
            firstStartStation = "출발지",
            lastEndStation = "도착지",
            steps = emptyList()
        )
        fakeRouteRepo.returnResult = TransitRouteResult.Success(listOf(sampleRoute))

        val viewModel = createViewModel()

        // Rapid requests
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        viewModel.loadRoute(RouteRequest.ToDestination(destMarket.id))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Success)
        assertEquals("시장", (state as RouteUiState.Success).destinationTitle)
        assertEquals(destMarket.latitude, fakeRouteRepo.lastEndLat)
        assertEquals(destMarket.longitude, fakeRouteRepo.lastEndLng)
    }

    @Test
    fun duplicateRequestWhileLoading_doesNotTriggerSecondCall() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        destRepo.saveDestination(testHospital)
        fakeRouteRepo.delayMs = 100

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id)) // Duplicate

        advanceUntilIdle()
        assertEquals(1, fakeRouteRepo.callCount)
    }

    @Test
    fun apiCallCountAbove30_doesNotBlockRouteSearch() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        destRepo.saveDestination(testHospital)
        val sampleRoute = TransitRoute(
            id = "r1",
            totalTime = 30,
            totalWalkDistance = 200,
            totalDistance = 5000.0,
            transferCount = 0,
            payment = 1400,
            firstStartStation = "강남역",
            lastEndStation = "혜화역",
            steps = emptyList()
        )
        fakeRouteRepo.returnResult = TransitRouteResult.Success(listOf(sampleRoute))

        // Pre-increment usage to 35
        for (i in 1..35) {
            apiUsageTracker.incrementUsage()
        }
        assertEquals(35, apiUsageTracker.getUsageCount())

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        advanceUntilIdle()

        assertEquals(1, fakeRouteRepo.callCount)
        assertTrue(viewModel.uiState.value is RouteUiState.Success)
    }

    @Test
    fun realtimeEnrichment_populatesRealtimeStatusWithoutFailingPrimaryRoute() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        destRepo.saveDestination(testHospital)

        val busStep = com.halmeoni.transit.domain.model.RouteStep(
            type = com.halmeoni.transit.domain.model.StepType.BUS,
            routeName = "661",
            startName = "시청앞",
            endName = "종로2가",
            startCityCode = 1000,
            startArsId = "16147"
        )
        val sampleRoute = TransitRoute(
            id = "r1",
            totalTime = 30,
            totalWalkDistance = 200,
            totalDistance = 5000.0,
            transferCount = 0,
            payment = 1400,
            firstStartStation = "시청앞",
            lastEndStation = "종로2가",
            steps = listOf(busStep)
        )
        fakeRouteRepo.returnResult = TransitRouteResult.Success(listOf(sampleRoute))

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Success)
        val success = state as RouteUiState.Success
        assertEquals(1, success.bestRoute.steps.size)
        // Primary route is always preserved
        assertNotNull(success.realtimeStatusMap[0])
    }

    @Test
    fun refreshRealtime_reloadsRealtimeTransitState() = runTest {
        settingsRepo.saveHomeLocation(testHome)
        destRepo.saveDestination(testHospital)

        val busStep = com.halmeoni.transit.domain.model.RouteStep(
            type = com.halmeoni.transit.domain.model.StepType.BUS,
            routeName = "661",
            startName = "시청앞",
            endName = "종로2가",
            startCityCode = 1000,
            startArsId = "16147"
        )
        val sampleRoute = TransitRoute(
            id = "r1",
            totalTime = 30,
            totalWalkDistance = 200,
            totalDistance = 5000.0,
            transferCount = 0,
            payment = 1400,
            firstStartStation = "시청앞",
            lastEndStation = "종로2가",
            steps = listOf(busStep)
        )
        fakeRouteRepo.returnResult = TransitRouteResult.Success(listOf(sampleRoute))

        val viewModel = createViewModel()
        viewModel.loadRoute(RouteRequest.ToDestination(testHospital.id))
        advanceUntilIdle()

        // Call manual refresh
        viewModel.refreshRealtime()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RouteUiState.Success)
    }
}
