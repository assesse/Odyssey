package com.halmeoni.transit.ui.admin

import androidx.lifecycle.ViewModel
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.model.HomeLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminSettings(
    val homeAddress: String = "",
    val homeLat: Double = 0.0,
    val homeLng: Double = 0.0,
    val isHomeConfigured: Boolean = false,
    val pin: String = "0000",
    val apiCallCount: Int = 0
)

class AdminViewModel(
    private val settingsRepository: SettingsRepository? = null,
    private val apiUsageTracker: ApiUsageTracker? = null
) : ViewModel() {
    private val _settings = MutableStateFlow(AdminSettings())
    val settings: StateFlow<AdminSettings> = _settings.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val home = settingsRepository?.getHomeLocation()
        val currentPin = settingsRepository?.getPin() ?: "0000"
        val count = apiUsageTracker?.getUsageCount() ?: 0

        _settings.value = AdminSettings(
            homeAddress = home?.address ?: "집 위치 미설정",
            homeLat = home?.latitude ?: 0.0,
            homeLng = home?.longitude ?: 0.0,
            isHomeConfigured = home != null,
            pin = currentPin,
            apiCallCount = count
        )
    }

    fun onPinDigitEntered(digit: String, onSuccess: () -> Unit) {
        if (_enteredPin.value.length < 4) {
            val updated = _enteredPin.value + digit
            _enteredPin.value = updated
            _pinError.value = false

            if (updated.length == 4) {
                val targetPin = settingsRepository?.getPin() ?: _settings.value.pin
                if (updated == targetPin) {
                    _enteredPin.value = ""
                    onSuccess()
                } else {
                    _pinError.value = true
                    _enteredPin.value = ""
                }
            }
        }
    }

    fun onPinClear() {
        _enteredPin.value = ""
        _pinError.value = false
    }

    fun updateHomeSettings(address: String, lat: Double, lng: Double) {
        settingsRepository?.saveHomeLocation(HomeLocation(lat, lng, address))
        _settings.value = _settings.value.copy(
            homeAddress = address,
            homeLat = lat,
            homeLng = lng,
            isHomeConfigured = true
        )
    }

    fun updatePin(newPin: String) {
        if (newPin.length == 4) {
            settingsRepository?.savePin(newPin)
            _settings.value = _settings.value.copy(pin = newPin)
        }
    }
}
