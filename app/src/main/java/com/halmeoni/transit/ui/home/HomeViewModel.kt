package com.halmeoni.transit.ui.home

import androidx.lifecycle.ViewModel
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.domain.model.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val destinations: List<Destination> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val destinationRepository: DestinationRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var logoTapCount = 0
    private var lastTapTimestamp = 0L

    init {
        loadDestinations()
    }

    fun loadDestinations() {
        val dests = destinationRepository?.getDestinations() ?: listOf(
            Destination("1", "서울대학교병원", "병원", 37.5796, 126.9990, "hospital", 1),
            Destination("2", "경동시장", "시장", 37.5804, 127.0385, "market", 2),
            Destination("3", "종로노인복지관", "복지관", 37.5760, 126.9980, "welfare", 3),
            Destination("4", "탑골공원", "공원", 37.5712, 126.9882, "park", 4)
        )
        _uiState.value = HomeUiState(destinations = dests)
    }

    fun onLogoTapped(onNavigateToPin: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastTapTimestamp > 2000) {
            logoTapCount = 1
        } else {
            logoTapCount++
        }
        lastTapTimestamp = now

        if (logoTapCount >= 5) {
            logoTapCount = 0
            onNavigateToPin()
        }
    }
}
