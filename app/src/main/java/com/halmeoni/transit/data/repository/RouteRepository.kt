package com.halmeoni.transit.data.repository

import com.halmeoni.transit.BuildConfig
import com.halmeoni.transit.data.api.OdsayApiService
import com.halmeoni.transit.domain.RouteMapper
import com.halmeoni.transit.domain.model.TransitRoute

interface RouteRepository {
    suspend fun getTransitRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Result<List<TransitRoute>>
}

class OdsayRouteRepository(
    private val apiService: OdsayApiService,
    private val routeMapper: RouteMapper = RouteMapper(),
    private val apiKeyProvider: () -> String = { BuildConfig.ODSAY_API_KEY }
) : RouteRepository {

    override suspend fun getTransitRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Result<List<TransitRoute>> {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank() || apiKey == "PLACEHOLDER_KEY" || apiKey == "PLACEHOLDER_ODSAY_API_KEY") {
            return Result.failure(IllegalStateException("ODSAY_API_KEY_NOT_CONFIGURED"))
        }

        return try {
            val response = apiService.searchPubTransPathT(
                SX = startLng,
                SY = startLat,
                EX = endLng,
                EY = endLat,
                apiKey = apiKey
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.result == null || body.result.path.isNullOrEmpty()) {
                    Result.success(emptyList())
                } else {
                    val domainRoutes = routeMapper.mapToDomain(body)
                    Result.success(domainRoutes)
                }
            } else {
                Result.failure(Exception("서버 응답 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
