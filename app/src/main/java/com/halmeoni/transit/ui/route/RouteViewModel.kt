package com.halmeoni.transit.ui.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halmeoni.transit.data.location.LocationProvider
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.RouteRepository
import com.halmeoni.transit.data.repository.RouteRepositoryError
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.data.repository.TransitRouteResult
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
    private val apiUsageTracker: ApiUsageTracker? = null,
    private val routeSelector: RouteSelector = RouteSelector()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private var currentSearchJob: Job? = null
    private var lastRequest: RouteRequest? = null

    fun loadRoute(request: RouteRequest) {
        // Prevent duplicate execution if already loading the same request
        if (_uiState.value is RouteUiState.Loading && lastRequest == request) {
            return
        }

        // Cancel existing job on new request
        currentSearchJob?.cancel()
        lastRequest = request

        val title = when (request) {
            is RouteRequest.GoHome -> "우리 집"
            is RouteRequest.ToDestination -> {
                destinationRepository.getDestinationById(request.destinationId)?.displayName ?: "목적지"
            }
        }

        _uiState.value = RouteUiState.Loading(destinationTitle = title)

        currentSearchJob = viewModelScope.launch {
            // 1. Verify Home Location configuration
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

            // 2. Resolve coordinates based on request type
            when (request) {
                is RouteRequest.GoHome -> {
                    val locationResult = locationProvider.getCurrentLocation(timeoutMs = 10_000L)
                    when (locationResult) {
                        is LocationResult.Success -> {
                            startLat = locationResult.latitude
                            startLng = locationResult.longitude
                            endLat = home.latitude
                            endLng = home.longitude
                        }
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
                        is LocationResult.Timeout, is LocationResult.Unavailable, is LocationResult.Cancelled -> {
                            _uiState.value = RouteUiState.Error(
                                errorType = RouteErrorType.LOCATION_UNAVAILABLE,
                                message = "현재 위치를 확인하지 못했어요.\n잠시 후 다시 시도해 주세요.",
                                destinationTitle = title
                            )
                            return@launch
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

            // 3. Search route via Repository
            val routeResult = routeRepository.getTransitRoutes(
                startLat = startLat,
                startLng = startLng,
                endLat = endLat,
                endLng = endLng
            )

            when (routeResult) {
                is TransitRouteResult.Success -> {
                    val routes = routeResult.routes
                    val selection = routeSelector.selectRoutes(routes)
                    if (selection.bestRoute != null) {
                        _uiState.value = RouteUiState.Success(
                            destinationTitle = title,
                            bestRoute = selection.bestRoute,
                            alternativeRoutes = selection.alternativeRoutes,
                            currentRouteIndex = 0
                        )
                    } else {
                        _uiState.value = RouteUiState.Error(
                            errorType = RouteErrorType.ROUTE_NOT_FOUND,
                            message = "지금 이용할 수 있는 대중교통 경로를 찾지 못했어요.",
                            destinationTitle = title
                        )
                    }
                }
                is TransitRouteResult.Failure -> {
                    val (errorType, message) = mapRepositoryErrorToUi(routeResult.error)
                    _uiState.value = RouteUiState.Error(
                        errorType = errorType,
                        message = message,
                        destinationTitle = title
                    )
                }
            }
        }
    }

    private fun mapRepositoryErrorToUi(error: RouteRepositoryError): Pair<RouteErrorType, String> {
        return when (error) {
            is RouteRepositoryError.ApiKeyNotConfigured -> {
                RouteErrorType.CONFIGURATION_REQUIRED to "대중교통 길찾기 API 키가 설정되지 않았어요.\n보호자 설정에서 ODsay API 키를 등록해 주세요."
            }
            is RouteRepositoryError.AuthenticationFailed -> {
                RouteErrorType.CONFIGURATION_REQUIRED to "대중교통 API 키 인증에 실패했어요.\n보호자 설정에서 키를 다시 확인해 주세요."
            }
            is RouteRepositoryError.NoStartStation -> {
                RouteErrorType.ROUTE_NOT_FOUND to "출발지 근처에서 이용할 수 있는\n버스나 지하철을 찾지 못했어요."
            }
            is RouteRepositoryError.NoEndStation -> {
                RouteErrorType.ROUTE_NOT_FOUND to "목적지 근처에서 이용할 수 있는\n버스나 지하철을 찾지 못했어요."
            }
            is RouteRepositoryError.NoStartAndEndStation -> {
                RouteErrorType.ROUTE_NOT_FOUND to "출발지와 목적지 근처에서\n이용할 수 있는 대중교통을 찾지 못했어요."
            }
            is RouteRepositoryError.UnsupportedArea -> {
                RouteErrorType.ROUTE_NOT_FOUND to "이 지역은 현재 길찾기를 지원하지 않아요."
            }
            is RouteRepositoryError.TooClose -> {
                RouteErrorType.ROUTE_NOT_FOUND to "목적지가 아주 가까이에 있어요.\n대중교통 길찾기가 필요하지 않을 수 있어요."
            }
            is RouteRepositoryError.NoRoute -> {
                RouteErrorType.ROUTE_NOT_FOUND to "지금 이용할 수 있는 대중교통 경로를 찾지 못했어요."
            }
            is RouteRepositoryError.ServerError,
            is RouteRepositoryError.NetworkError,
            is RouteRepositoryError.Timeout -> {
                RouteErrorType.NETWORK_ERROR to "길찾기 서버에 연결하지 못했어요.\n잠시 후 다시 시도해 주세요."
            }
            is RouteRepositoryError.InvalidParameter,
            is RouteRepositoryError.MissingParameter,
            is RouteRepositoryError.Unknown -> {
                RouteErrorType.UNKNOWN_ERROR to "경로를 찾는 중 오류가 발생했어요.\n잠시 후 다시 시도해 주세요."
            }
        }
    }

    fun toggleNextRoute() {
        val currentState = _uiState.value
        if (currentState is RouteUiState.Success) {
            val totalRoutes = currentState.totalRouteCount
            if (totalRoutes > 1) {
                val nextIndex = (currentState.currentRouteIndex + 1) % totalRoutes
                _uiState.value = currentState.copy(currentRouteIndex = nextIndex)
            }
        }
    }

    fun retry() {
        val req = lastRequest
        if (req != null) {
            _uiState.value = RouteUiState.Idle
            loadRoute(req)
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentSearchJob?.cancel()
    }
}
