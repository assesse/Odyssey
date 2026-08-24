package com.halmeoni.transit

import com.halmeoni.transit.data.api.OdsayApiService
import com.halmeoni.transit.data.api.OdsayResponse
import com.halmeoni.transit.data.repository.OdsayRouteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RouteRepositoryTest {

    private class MockOdsayApiService : OdsayApiService {
        var passedSX: Double? = null
        var passedSY: Double? = null
        var passedEX: Double? = null
        var passedEY: Double? = null
        var passedApiKey: String? = null

        override suspend fun searchPubTransPathT(
            SX: Double,
            SY: Double,
            EX: Double,
            EY: Double,
            apiKey: String,
            OPT: Int
        ): Response<OdsayResponse> {
            passedSX = SX
            passedSY = SY
            passedEX = EX
            passedEY = EY
            passedApiKey = apiKey
            return Response.success(OdsayResponse(result = null))
        }
    }

    @Test
    fun getTransitRoutes_whenApiKeyIsPlaceholder_returnsFailureWithoutCallingApi() = runBlocking {
        val mockApi = MockOdsayApiService()
        val repo = OdsayRouteRepository(
            apiService = mockApi,
            apiKeyProvider = { "PLACEHOLDER_KEY" }
        )

        val result = repo.getTransitRoutes(
            startLat = 37.5000,
            startLng = 127.0000,
            endLat = 37.6000,
            endLng = 127.1000
        )

        assertTrue(result.isFailure)
        assertEquals("ODSAY_API_KEY_NOT_CONFIGURED", result.exceptionOrNull()?.message)
        assertEquals(null, mockApi.passedApiKey)
    }

    @Test
    fun getTransitRoutes_mapsCoordinatesCorrectlyToOdsayParams() = runBlocking {
        val mockApi = MockOdsayApiService()
        val repo = OdsayRouteRepository(
            apiService = mockApi,
            apiKeyProvider = { "VALID_SECRET_KEY" }
        )

        val startLat = 37.1234
        val startLng = 127.5678
        val endLat = 37.8765
        val endLng = 127.4321

        val result = repo.getTransitRoutes(
            startLat = startLat,
            startLng = startLng,
            endLat = endLat,
            endLng = endLng
        )

        assertTrue(result.isSuccess)
        // SX = startLng, SY = startLat, EX = endLng, EY = endLat
        assertEquals(startLng, mockApi.passedSX!!, 0.00001)
        assertEquals(startLat, mockApi.passedSY!!, 0.00001)
        assertEquals(endLng, mockApi.passedEX!!, 0.00001)
        assertEquals(endLat, mockApi.passedEY!!, 0.00001)
        assertEquals("VALID_SECRET_KEY", mockApi.passedApiKey)
    }
}
