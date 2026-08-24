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
        val dests = destinationRepository?.getDestinations() ?: emptyList()
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
