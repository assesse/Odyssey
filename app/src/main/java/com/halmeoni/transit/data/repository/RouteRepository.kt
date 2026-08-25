package com.halmeoni.transit.data.repository

import com.halmeoni.transit.BuildConfig
import com.halmeoni.transit.data.api.OdsayApiService
import com.halmeoni.transit.data.api.OdsayErrorItem
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.RouteMapper
import com.halmeoni.transit.domain.model.TransitRoute
import java.io.IOException
import java.net.SocketTimeoutException

sealed interface RouteRepositoryError {
    data object ApiKeyNotConfigured : RouteRepositoryError
    data object AuthenticationFailed : RouteRepositoryError

    data object InvalidParameter : RouteRepositoryError       // -8
    data object MissingParameter : RouteRepositoryError       // -9

    data object NoStartStation : RouteRepositoryError         // 3
    data object NoEndStation : RouteRepositoryError           // 4
    data object NoStartAndEndStation : RouteRepositoryError   // 5

    data object UnsupportedArea : RouteRepositoryError        // 6
    data object TooClose : RouteRepositoryError               // -98
    data object NoRoute : RouteRepositoryError                // -99

    data object ServerError : RouteRepositoryError            // 500
    data object NetworkError : RouteRepositoryError
    data object Timeout : RouteRepositoryError

    data class Unknown(
        val code: String?,
        val message: String?
    ) : RouteRepositoryError
}

sealed interface TransitRouteResult {
    data class Success(val routes: List<TransitRoute>) : TransitRouteResult
    data class Failure(val error: RouteRepositoryError) : TransitRouteResult
}

interface RouteRepository {
    suspend fun getTransitRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): TransitRouteResult
}

class OdsayRouteRepository(
    private val apiService: OdsayApiService,
    private val routeMapper: RouteMapper = RouteMapper(),
    private val apiUsageTracker: ApiUsageTracker? = null,
    private val apiKeyProvider: () -> String = { BuildConfig.ODSAY_API_KEY }
) : RouteRepository {

    override suspend fun getTransitRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): TransitRouteResult {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank() || apiKey == "PLACEHOLDER_KEY" || apiKey == "PLACEHOLDER_ODSAY_API_KEY") {
            return TransitRouteResult.Failure(RouteRepositoryError.ApiKeyNotConfigured)
        }

        // Count API call attempt
        apiUsageTracker?.incrementUsage()

        return try {
            val response = apiService.searchPubTransPathT(
                apiKey = apiKey,
                SX = startLng,
                SY = startLat,
                EX = endLng,
                EY = endLat,
                lang = 0,
                output = "json",
                OPT = 0,
                SearchType = 0,
                SearchPathType = 0
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Check for error payload first
                if (!body.error.isNullOrEmpty()) {
                    val firstError = body.error.first()
                    return TransitRouteResult.Failure(mapOdsayError(firstError))
                }

                if (body.result == null || body.result.path.isNullOrEmpty()) {
                    return TransitRouteResult.Failure(RouteRepositoryError.NoRoute)
                }

                val domainRoutes = routeMapper.mapToDomain(body)
                if (domainRoutes.isEmpty()) {
                    TransitRouteResult.Failure(RouteRepositoryError.NoRoute)
                } else {
                    TransitRouteResult.Success(domainRoutes)
                }
            } else {
                TransitRouteResult.Failure(RouteRepositoryError.ServerError)
            }
        } catch (e: SocketTimeoutException) {
            TransitRouteResult.Failure(RouteRepositoryError.Timeout)
        } catch (e: IOException) {
            TransitRouteResult.Failure(RouteRepositoryError.NetworkError)
        } catch (e: Exception) {
            TransitRouteResult.Failure(RouteRepositoryError.Unknown(code = null, message = e.message))
        }
    }

    private fun mapOdsayError(errorItem: OdsayErrorItem): RouteRepositoryError {
        val code = errorItem.code?.trim()
        val msg = errorItem.displayMessage ?: ""

        // Check for explicit authentication failure
        if (msg.contains("ApiKeyAuthFailed", ignoreCase = true) ||
            msg.contains("authentication failed", ignoreCase = true) ||
            msg.contains("인증", ignoreCase = true) && code == "500"
        ) {
            return RouteRepositoryError.AuthenticationFailed
        }

        return when (code) {
            "500" -> RouteRepositoryError.ServerError
            "-8" -> RouteRepositoryError.InvalidParameter
            "-9" -> RouteRepositoryError.MissingParameter
            "3" -> RouteRepositoryError.NoStartStation
            "4" -> RouteRepositoryError.NoEndStation
            "5" -> RouteRepositoryError.NoStartAndEndStation
            "6" -> RouteRepositoryError.UnsupportedArea
            "-98" -> RouteRepositoryError.TooClose
            "-99" -> RouteRepositoryError.NoRoute
            else -> {
                if (msg.contains("ApiKeyAuthFailed", ignoreCase = true)) {
                    RouteRepositoryError.AuthenticationFailed
                } else {
                    RouteRepositoryError.Unknown(code = code, message = msg)
                }
            }
        }
    }
}
