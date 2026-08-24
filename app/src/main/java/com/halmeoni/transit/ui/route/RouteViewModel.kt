package com.halmeoni.transit.ui.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halmeoni.transit.data.location.LocationProvider
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.RouteRepository
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.RouteSelector
import com.halmeoni.transit.domain.model.LocationResult
import com.halmeoni.transit.domain.model.RouteRequest
import com.halmeoni.transit.domain.model.TransitRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RouteErrorType {
    CONFIGURATION_REQUIRED,
    PERMISSION_REQUIRED,
    LOCATION_SERVICE_DISABLED,
    LOCATION_UNAVAILABLE,
    ROUTE_NOT_FOUND,
    DAILY_LIMIT_EXCEEDED,
    NETWORK_ERROR,
    UNKNOWN_ERROR
}

sealed interface RouteUiState {
    data object Idle : RouteUiState

    data class Loading(
        val destinationTitle: String
    ) : RouteUiState

    data class Success(
        val destinationTitle: String,
        val bestRoute: TransitRoute,
        val alternativeRoutes: List<TransitRoute> = emptyList(),
        val currentRouteIndex: Int = 0
    ) : RouteUiState {
        val currentDisplayRoute: TransitRoute
            get() = if (currentRouteIndex == 0) bestRoute else alternativeRoutes.getOrElse(currentRouteIndex - 1) { bestRoute }

        val totalRouteCount: Int
            get() = 1 + alternativeRoutes.size
    }

    data class Error(
        val errorType: RouteErrorType,
        val message: String,
        val destinationTitle: String = ""
    ) : RouteUiState
}

class RouteViewModel(
    private val routeRepository: RouteRepository,
    private val locationProvider: LocationProvider,
    private val destinationRepository: DestinationRepository,
    private val settingsRepository: SettingsRepository,
    private val apiUsageTracker: ApiUsageTracker,
    private val routeSelector: RouteSelector = RouteSelector()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private var currentSearchJob: Job? = null
    private var lastRequested: RouteRequest? = null

    fun loadRoute(request: RouteRequest) {
        if (_uiState.value is RouteUiState.Loading && lastRequested == request) {
            return
        }

        currentSearchJob?.cancel()
        lastRequested = request

        val title = when (request) {
            is RouteRequest.GoHome -> "우리 집"
            is RouteRequest.ToDestination -> {
                destinationRepository.getDestinationById(request.destinationId)?.displayName
                    ?: "목적지"
            }
        }

        _uiState.value = RouteUiState.Loading(destinationTitle = title)

        currentSearchJob = viewModelScope.launch {
            val home = settingsRepository.getHomeLocation()
            if (home == null) {
                _uiState.value = RouteUiState.Error(
                    errorType = RouteErrorType.CONFIGURATION_REQUIRED,
                    message = "집 위치가 아직 등록되지 않았어요.\n보호자 설정에서 집 위치를 먼저 등록해 주세요.",
                    destinationTitle = title
                )
                return@launch
            }

            val startLat: Double
            val startLng: Double
            val endLat: Double
            val endLng: Double

            when (request) {
                is RouteRequest.GoHome -> {
                    when (val locResult = locationProvider.getCurrentLocation()) {
                        is LocationResult.PermissionDenied -> {
                            _uiState.value = RouteUiState.Error(
                                errorType = RouteErrorType.PERMISSION_REQUIRED,
                                message = "현재 위치를 확인하려면 위치 권한을 허용해 주세요.",
                                destinationTitle = title
                            )
                            return@launch
                        }
                        is LocationResult.LocationServiceDisabled -> {
                            _uiState.value = RouteUiState.Error(
                                errorType = RouteErrorType.LOCATION_SERVICE_DISABLED,
                                message = "휴대전화의 위치 기능(GPS)을 켜 주세요.",
                                destinationTitle = title
                            )
                            return@launch
                        }
                        is LocationResult.Timeout,
                        is LocationResult.Unavailable -> {
                            _uiState.value = RouteUiState.Error(
                                errorType = RouteErrorType.LOCATION_UNAVAILABLE,
                                message = "현재 위치를 확인하지 못했어요. 다시 시도해 주세요.",
                                destinationTitle = title
                            )
                            return@launch
                        }
                        is LocationResult.Cancelled -> {
                            _uiState.value = RouteUiState.Error(
                                errorType = RouteErrorType.LOCATION_UNAVAILABLE,
                                message = "위치 확인이 취소되었어요.",
                                destinationTitle = title
                            )
                            return@launch
                        }
                        is LocationResult.Success -> {
                            startLat = locResult.latitude
                            startLng = locResult.longitude
                            endLat = home.latitude
                            endLng = home.longitude
                        }
                    }
                }
                is RouteRequest.ToDestination -> {
                    val dest = destinationRepository.getDestinationById(request.destinationId)
                    if (dest == null) {
                        _uiState.value = RouteUiState.Error(
                            errorType = RouteErrorType.CONFIGURATION_REQUIRED,
                            message = "목적지가 아직 등록되지 않았어요.",
                            destinationTitle = title
                        )
                        return@launch
                    }
                    startLat = home.latitude
                    startLng = home.longitude
                    endLat = dest.latitude
                    endLng = dest.longitude
                }
            }

            if (!apiUsageTracker.canMakeApiCall()) {
                _uiState.value = RouteUiState.Error(
                    errorType = RouteErrorType.DAILY_LIMIT_EXCEEDED,
                    message = "오늘은 더 이상 길을 찾을 수 없어요. (일일 한도 30회 초과)\n내일 다시 이용해 주세요.",
                    destinationTitle = title
                )
                return@launch
            }

            val routesResult = routeRepository.getTransitRoutes(
                startLat = startLat,
                startLng = startLng,
                endLat = endLat,
                endLng = endLng
            )

            routesResult.fold(
                onSuccess = { routes ->
                    apiUsageTracker.incrementUsage()
                    val selectionResult = routeSelector.selectRoutes(routes)

                    if (selectionResult.bestRoute != null) {
                        _uiState.value = RouteUiState.Success(
                            destinationTitle = title,
                            bestRoute = selectionResult.bestRoute,
                            alternativeRoutes = selectionResult.alternativeRoutes,
                            currentRouteIndex = 0
                        )
                    } else {
                        _uiState.value = RouteUiState.Error(
                            errorType = RouteErrorType.ROUTE_NOT_FOUND,
                            message = "지금은 이용할 수 있는 대중교통 경로가 없어요.",
                            destinationTitle = title
                        )
                    }
                },
                onFailure = { error ->
                    val (type, message) = if (error.message == "ODSAY_API_KEY_NOT_CONFIGURED") {
                        RouteErrorType.CONFIGURATION_REQUIRED to "길찾기 설정(API 키)이 완료되지 않았어요."
                    } else {
                        RouteErrorType.NETWORK_ERROR to "길 찾기 서버에 연결할 수 없어요.\n잠시 후 다시 시도해 주세요."
                    }
                    _uiState.value = RouteUiState.Error(
                        errorType = type,
                        message = message,
                        destinationTitle = title
                    )
                }
            )
        }
    }

    fun toggleNextRoute() {
        val current = _uiState.value
        if (current is RouteUiState.Success && current.totalRouteCount > 1) {
            val nextIndex = (current.currentRouteIndex + 1) % current.totalRouteCount
            _uiState.value = current.copy(currentRouteIndex = nextIndex)
        }
    }

    fun retry() {
        val req = lastRequested
        if (req != null) {
            loadRoute(req)
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentSearchJob?.cancel()
    }
}
