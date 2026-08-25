package com.halmeoni.transit.ui.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halmeoni.transit.data.location.LocationProvider
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.RealtimeTransitRepository
import com.halmeoni.transit.data.repository.RouteRepository
import com.halmeoni.transit.data.repository.RouteRepositoryError
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.data.repository.TransitRouteResult
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.RouteSelector
import com.halmeoni.transit.domain.model.LocationResult
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteRequest
import com.halmeoni.transit.domain.model.StepType
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
        val currentRouteIndex: Int = 0,
        val realtimeStatusMap: Map<Int, RealtimeStatus> = emptyMap(),
        val isRealtimeLoading: Boolean = false
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
    private val routeSelector: RouteSelector = RouteSelector(),
    private val realtimeTransitRepository: RealtimeTransitRepository = RealtimeTransitRepository(settingsRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private var currentSearchJob: Job? = null
    private var currentRealtimeJob: Job? = null
    private var lastRequest: RouteRequest? = null

    fun loadRoute(request: RouteRequest) {
        if (currentSearchJob?.isActive == true && lastRequest == request) {
            return
        }

        lastRequest = request
        currentSearchJob?.cancel()
        currentRealtimeJob?.cancel()

        val title = when (request) {
            is RouteRequest.GoHome -> "우리 집"
            is RouteRequest.ToDestination -> {
                destinationRepository.getDestinationById(request.destinationId)?.displayName ?: "목적지"
            }
        }

        currentSearchJob = viewModelScope.launch {
            _uiState.value = RouteUiState.Loading(destinationTitle = title)

            // 1. Check API Key
            if (!settingsRepository.isApiKeyConfigured()) {
                _uiState.value = RouteUiState.Error(
                    errorType = RouteErrorType.CONFIGURATION_REQUIRED,
                    message = "보호자 설정에서 대중교통(ODsay) API 키를 먼저 등록해 주세요.",
                    destinationTitle = title
                )
                return@launch
            }

            // 2. Check Home Location
            val home = settingsRepository.getHomeLocation()
            if (home == null) {
                _uiState.value = RouteUiState.Error(
                    errorType = RouteErrorType.CONFIGURATION_REQUIRED,
                    message = "보호자 설정에서 집 위치를 먼저 등록해 주세요.",
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
                    endLat = home.latitude
                    endLng = home.longitude

                    when (val locResult = locationProvider.getCurrentLocation()) {
                        is LocationResult.Success -> {
                            startLat = locResult.latitude
                            startLng = locResult.longitude
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
                                message = "스마트폰의 위치(GPS) 기능을 켜주세요.",
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
                        val successState = RouteUiState.Success(
                            destinationTitle = title,
                            bestRoute = selection.bestRoute,
                            alternativeRoutes = selection.alternativeRoutes,
                            currentRouteIndex = 0
                        )
                        _uiState.value = successState

                        // Enrich realtime information non-blockingly
                        enrichRealtimeTransit(selection.bestRoute, forceRefresh = false)
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

    fun enrichRealtimeTransit(route: TransitRoute, forceRefresh: Boolean = false) {
        currentRealtimeJob?.cancel()

        currentRealtimeJob = viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is RouteUiState.Success) return@launch

            // Initialize loading statuses for bus/subway steps
            val initialStatusMap = currentState.realtimeStatusMap.toMutableMap()
            route.steps.forEachIndexed { index, step ->
                if (step.type == StepType.BUS || step.type == StepType.SUBWAY) {
                    initialStatusMap[index] = RealtimeStatus.Loading
                }
            }
            _uiState.value = currentState.copy(
                realtimeStatusMap = initialStatusMap,
                isRealtimeLoading = true
            )

            // Fetch realtime arrival for each transit step
            val updatedMap = initialStatusMap.toMutableMap()
            for ((index, step) in route.steps.withIndex()) {
                if (step.type == StepType.BUS || step.type == StepType.SUBWAY) {
                    val status = try {
                        realtimeTransitRepository.getRealtimeArrival(step, forceRefresh = forceRefresh)
                    } catch (e: Exception) {
                        RealtimeStatus.NetworkError("실시간 정보 확인 불가")
                    }
                    updatedMap[index] = status

                    val latestState = _uiState.value
                    if (latestState is RouteUiState.Success) {
                        _uiState.value = latestState.copy(realtimeStatusMap = updatedMap.toMap())
                    }
                }
            }

            val finalState = _uiState.value
            if (finalState is RouteUiState.Success) {
                _uiState.value = finalState.copy(isRealtimeLoading = false)
            }
        }
    }

    fun refreshRealtime() {
        val state = _uiState.value
        if (state is RouteUiState.Success) {
            enrichRealtimeTransit(state.currentDisplayRoute, forceRefresh = true)
        }
    }

    fun toggleNextRoute() {
        val currentState = _uiState.value
        if (currentState is RouteUiState.Success && currentState.totalRouteCount > 1) {
            val nextIndex = (currentState.currentRouteIndex + 1) % currentState.totalRouteCount
            val nextState = currentState.copy(
                currentRouteIndex = nextIndex,
                realtimeStatusMap = emptyMap(),
                isRealtimeLoading = false
            )
            _uiState.value = nextState
            enrichRealtimeTransit(nextState.currentDisplayRoute, forceRefresh = false)
        }
    }

    fun retry() {
        lastRequest?.let { loadRoute(it) }
    }

    fun cancelSearch() {
        currentSearchJob?.cancel()
        currentRealtimeJob?.cancel()
        _uiState.value = RouteUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        currentSearchJob?.cancel()
        currentRealtimeJob?.cancel()
    }

    private fun mapRepositoryErrorToUi(error: RouteRepositoryError): Pair<RouteErrorType, String> {
        return when (error) {
            is RouteRepositoryError.ApiKeyNotConfigured -> {
                Pair(RouteErrorType.CONFIGURATION_REQUIRED, "보호자 설정에서 ODsay API 키를 먼저 등록해 주세요.")
            }
            is RouteRepositoryError.AuthenticationFailed -> {
                Pair(RouteErrorType.CONFIGURATION_REQUIRED, "ODsay API 키 인증에 실패했습니다.\n보호자 설정에서 키를 확인해 주세요.")
            }
            is RouteRepositoryError.InvalidParameter, is RouteRepositoryError.MissingParameter -> {
                Pair(RouteErrorType.UNKNOWN_ERROR, "경로 요청 정보가 올바르지 않습니다.")
            }
            is RouteRepositoryError.NoStartStation, is RouteRepositoryError.NoEndStation, is RouteRepositoryError.NoStartAndEndStation -> {
                Pair(RouteErrorType.ROUTE_NOT_FOUND, "출발지 또는 도착지 주변 정류장을 찾을 수 없습니다.")
            }
            is RouteRepositoryError.UnsupportedArea -> {
                Pair(RouteErrorType.ROUTE_NOT_FOUND, "대중교통 정보가 지원되지 않는 지역입니다.")
            }
            is RouteRepositoryError.TooClose -> {
                Pair(RouteErrorType.ROUTE_NOT_FOUND, "출발지와 도착지가 너무 가깝습니다.")
            }
            is RouteRepositoryError.NoRoute -> {
                Pair(RouteErrorType.ROUTE_NOT_FOUND, "지금 이용할 수 있는 대중교통 경로가 없어요.")
            }
            is RouteRepositoryError.ServerError -> {
                Pair(RouteErrorType.NETWORK_ERROR, "대중교통 서버 응답 오류가 발생했어요.\n잠시 후 다시 시도해 주세요.")
            }
            is RouteRepositoryError.NetworkError -> {
                Pair(RouteErrorType.NETWORK_ERROR, "인터넷 연결을 확인하고 다시 시도해 주세요.")
            }
            is RouteRepositoryError.Timeout -> {
                Pair(RouteErrorType.NETWORK_ERROR, "대중교통 서버 응답 시간이 초과되었습니다.\n잠시 후 다시 시도해 주세요.")
            }
            is RouteRepositoryError.Unknown -> {
                Pair(RouteErrorType.UNKNOWN_ERROR, "경로를 찾지 못했어요: ${error.message ?: "알 수 없는 오류"}")
            }
        }
    }
}
