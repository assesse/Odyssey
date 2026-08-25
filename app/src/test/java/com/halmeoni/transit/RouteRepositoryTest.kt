package com.halmeoni.transit

import com.halmeoni.transit.data.api.OdsayApiService
import com.halmeoni.transit.data.api.OdsayErrorItem
import com.halmeoni.transit.data.api.OdsayResponse
import com.halmeoni.transit.data.repository.OdsayRouteRepository
import com.halmeoni.transit.data.repository.RouteRepositoryError
import com.halmeoni.transit.data.repository.TransitRouteResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RouteRepositoryTest {

    private class MockOdsayApiService(
        var responseToReturn: Response<OdsayResponse> = Response.success(OdsayResponse(result = null))
    ) : OdsayApiService {
        var passedSX: Double? = null
        var passedSY: Double? = null
        var passedEX: Double? = null
        var passedEY: Double? = null
        var passedApiKey: String? = null
        var passedLang: Int? = null
        var passedOutput: String? = null
        var passedOPT: Int? = null
        var passedSearchType: Int? = null
        var passedSearchPathType: Int? = null

        override suspend fun searchPubTransPathT(
            apiKey: String,
            SX: Double,
            SY: Double,
            EX: Double,
            EY: Double,
            lang: Int,
            output: String,
            OPT: Int,
            SearchType: Int,
            SearchPathType: Int
        ): Response<OdsayResponse> {
            passedApiKey = apiKey
            passedSX = SX
            passedSY = SY
            passedEX = EX
            passedEY = EY
            passedLang = lang
            passedOutput = output
            passedOPT = OPT
            passedSearchType = SearchType
            passedSearchPathType = SearchPathType
            return responseToReturn
        }
    }

    @Test
    fun getTransitRoutes_whenApiKeyIsPlaceholder_returnsFailureApiKeyNotConfigured() = runBlocking {
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

        assertTrue(result is TransitRouteResult.Failure)
        assertEquals(RouteRepositoryError.ApiKeyNotConfigured, (result as TransitRouteResult.Failure).error)
        assertEquals(null, mockApi.passedApiKey)
    }

    @Test
    fun getTransitRoutes_mapsCoordinatesAndParametersCorrectlyToOdsayContract() = runBlocking {
        val mockApi = MockOdsayApiService()
        val repo = OdsayRouteRepository(
            apiService = mockApi,
            apiKeyProvider = { "VALID_SECRET_KEY" }
        )

        val startLat = 37.1234
        val startLng = 127.5678
        val endLat = 37.8765
        val endLng = 127.4321

        repo.getTransitRoutes(
            startLat = startLat,
            startLng = startLng,
            endLat = endLat,
            endLng = endLng
        )

        assertEquals("VALID_SECRET_KEY", mockApi.passedApiKey)
        // SX = startLng, SY = startLat, EX = endLng, EY = endLat
        assertEquals(startLng, mockApi.passedSX!!, 0.00001)
        assertEquals(startLat, mockApi.passedSY!!, 0.00001)
        assertEquals(endLng, mockApi.passedEX!!, 0.00001)
        assertEquals(endLat, mockApi.passedEY!!, 0.00001)
        assertEquals(0, mockApi.passedLang)
        assertEquals("json", mockApi.passedOutput)
        assertEquals(0, mockApi.passedOPT)
        assertEquals(0, mockApi.passedSearchType)
        assertEquals(0, mockApi.passedSearchPathType)
    }

    @Test
    fun getTransitRoutes_mapsAllOfficialErrorCodes() = runBlocking {
        val testCases = mapOf(
            "500" to RouteRepositoryError.ServerError,
            "-8" to RouteRepositoryError.InvalidParameter,
            "-9" to RouteRepositoryError.MissingParameter,
            "3" to RouteRepositoryError.NoStartStation,
            "4" to RouteRepositoryError.NoEndStation,
            "5" to RouteRepositoryError.NoStartAndEndStation,
            "6" to RouteRepositoryError.UnsupportedArea,
            "-98" to RouteRepositoryError.TooClose,
            "-99" to RouteRepositoryError.NoRoute
        )

        for ((code, expectedError) in testCases) {
            val errorResponse = OdsayResponse(
                error = listOf(OdsayErrorItem(code = code, msg = "오류 메시지"))
            )
            val mockApi = MockOdsayApiService(responseToReturn = Response.success(errorResponse))
            val repo = OdsayRouteRepository(
                apiService = mockApi,
                apiKeyProvider = { "VALID_KEY" }
            )

            val result = repo.getTransitRoutes(37.5, 127.0, 37.6, 127.1)
            assertTrue("Code $code should map to $expectedError", result is TransitRouteResult.Failure)
            assertEquals(expectedError, (result as TransitRouteResult.Failure).error)
        }
    }

    @Test
    fun getTransitRoutes_mapsAuthenticationFailureError() = runBlocking {
        val authErrorResponse = OdsayResponse(
            error = listOf(OdsayErrorItem(code = "500", msg = "[ApiKeyAuthFailed] ApiKey authentication failed."))
        )
        val mockApi = MockOdsayApiService(responseToReturn = Response.success(authErrorResponse))
        val repo = OdsayRouteRepository(
            apiService = mockApi,
            apiKeyProvider = { "INVALID_KEY" }
        )

        val result = repo.getTransitRoutes(37.5, 127.0, 37.6, 127.1)
        assertTrue(result is TransitRouteResult.Failure)
        assertEquals(RouteRepositoryError.AuthenticationFailed, (result as TransitRouteResult.Failure).error)
    }
}
