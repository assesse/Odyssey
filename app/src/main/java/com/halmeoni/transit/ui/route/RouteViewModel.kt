package com.halmeoni.transit.ui.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halmeoni.transit.BuildConfig
import com.halmeoni.transit.data.api.OdsayApiService
import com.halmeoni.transit.data.location.LocationProvider
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.RouteMapper
import com.halmeoni.transit.domain.RouteSelector
import com.halmeoni.transit.domain.model.Destination
import com.halmeoni.transit.domain.model.TransitRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RouteUiState(
    val destinationName: String = "",
    val bestRoute: TransitRoute? = null,
    val alternativeRoutes: List<TransitRoute> = emptyList(),
    val currentRouteIndex: Int = 0, // 0 = bestRoute, 1+ = alternativeRoutes
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val currentDisplayRoute: TransitRoute?
        get() = if (currentRouteIndex == 0) bestRoute else alternativeRoutes.getOrNull(currentRouteIndex - 1)

    val totalRouteCount: Int
        get() = (if (bestRoute != null) 1 else 0) + alternativeRoutes.size
}

class RouteViewModel(
    private val odsayApiService: OdsayApiService? = null,
    private val locationProvider: LocationProvider? = null,
    private val destinationRepository: DestinationRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val apiUsageTracker: ApiUsageTracker? = null,
    private val routeMapper: RouteMapper = RouteMapper(),
    private val routeSelector: RouteSelector = RouteSelector()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    fun loadRoute(destinationName: String, targetDestination: Destination? = null) {
        _uiState.value = _uiState.value.copy(
            destinationName = destinationName,
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            if (apiUsageTracker != null && !apiUsageTracker.canMakeApiCall()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "오늘은 더 이상 길을 찾을 수 없어요. (일일 한도 30회 초과)\n내일 다시 이용해 주세요."
                )
                return@launch
            }

            val home = settingsRepository?.getHomeLocation()
            val homeLat = home?.latitude ?: 37.5665
            val homeLng = home?.longitude ?: 126.9780

            val userLocation = locationProvider?.getCurrentLocation(
                homeLat = homeLat,
                homeLng = homeLng
            )

            val startLat = userLocation?.latitude ?: homeLat
            val startLng = userLocation?.longitude ?: homeLng

            val dest = targetDestination ?: destinationRepository?.getDestinations()?.find {
                it.displayName == destinationName || it.name == destinationName
            } ?: Destination("target", destinationName, destinationName, 37.5796, 126.9990, "hospital", 1)

            val apiKey = BuildConfig.ODSAY_API_KEY.ifBlank { "PLACEHOLDER_KEY" }

            if (odsayApiService != null && apiKey != "PLACEHOLDER_KEY") {
                try {
                    val response = odsayApiService.searchPubTransPathT(
                        SX = startLng,
                        SY = startLat,
                        EX = dest.longitude,
                        EY = dest.latitude,
                        apiKey = apiKey
                    )

                    if (response.isSuccessful && response.body() != null) {
                        apiUsageTracker?.incrementUsage()
                        val domainRoutes = routeMapper.mapToDomain(response.body()!!)
                        val selectionResult = routeSelector.selectRoutes(domainRoutes)

                        if (selectionResult.bestRoute != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                bestRoute = selectionResult.bestRoute,
                                alternativeRoutes = selectionResult.alternativeRoutes,
                                currentRouteIndex = 0,
                                errorMessage = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "지금은 이용할 수 있는 대중교통 경로가 없어요."
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "길 찾기 서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요."
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "인터넷 연결이 불안정하거나 오류가 발생했어요."
                    )
                }
            } else {
                // Fallback Sample Route for testing/development when API key is placeholder
                val sampleBest = TransitRoute(
                    id = "sample_1",
                    totalTime = 25,
                    totalWalkDistance = 350,
                    totalDistance = 6500,
                    transferCount = 0,
                    payment = 1400,
                    firstStartStation = "시청역 정류장",
                    lastEndStation = "${destinationName} 정류장",
                    steps = emptyList()
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bestRoute = sampleBest,
                    alternativeRoutes = emptyList(),
                    currentRouteIndex = 0,
                    errorMessage = null
                )
            }
        }
    }

    fun toggleNextRoute() {
        val state = _uiState.value
        val total = state.totalRouteCount
        if (total > 1) {
            val nextIndex = (state.currentRouteIndex + 1) % total
            _uiState.value = state.copy(currentRouteIndex = nextIndex)
        }
    }
}
