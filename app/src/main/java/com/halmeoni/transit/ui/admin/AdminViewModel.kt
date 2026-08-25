package com.halmeoni.transit.ui.admin

import androidx.lifecycle.ViewModel
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.model.Destination
import com.halmeoni.transit.domain.model.HomeLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AdminSettingsState(
    // Home location fields
    val homeAddressInput: String = "",
    val homeLatInput: String = "",
    val homeLngInput: String = "",
    val isHomeConfigured: Boolean = false,
    val homeAddressError: String? = null,
    val homeLatError: String? = null,
    val homeLngError: String? = null,

    // Destination list
    val destinations: List<Destination> = emptyList(),

    // Destination dialog/edit fields
    val isDestDialogVisible: Boolean = false,
    val editingDestId: String? = null,
    val destShortNameInput: String = "",
    val destFullNameInput: String = "",
    val destLatInput: String = "",
    val destLngInput: String = "",
    val destShortNameError: String? = null,
    val destFullNameError: String? = null,
    val destLatError: String? = null,
    val destLngError: String? = null,
    val destGeneralError: String? = null,

    // ODsay API Key
    val apiKeyInput: String = "",
    val isApiKeyConfigured: Boolean = false,
    val apiKeyError: String? = null,

    // Public Data Bus API Key (Seoul/Gyeonggi)
    val busApiKeyInput: String = "",
    val isBusApiKeyConfigured: Boolean = false,

    // Seoul Subway API Key
    val subwayApiKeyInput: String = "",
    val isSubwayApiKeyConfigured: Boolean = false,

    // PIN change fields
    val newPinInput: String = "",
    val confirmPinInput: String = "",
    val pinChangeError: String? = null,

    // General save error/status
    val saveErrorMessage: String? = null,
    val apiCallCount: Int = 0
)

class AdminViewModel(
    private val settingsRepository: SettingsRepository,
    private val destinationRepository: DestinationRepository,
    private val apiUsageTracker: ApiUsageTracker
) : ViewModel() {

    private val _settings = MutableStateFlow(AdminSettingsState())
    val settings: StateFlow<AdminSettingsState> = _settings.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val home = settingsRepository.getHomeLocation()
        val dests = destinationRepository.getDestinations()
        val count = apiUsageTracker.getUsageCount()
        val apiKey = settingsRepository.getApiKey()
        val isApiKeyOk = settingsRepository.isApiKeyConfigured()
        val busApiKey = settingsRepository.getBusApiKey()
        val isBusApiKeyOk = settingsRepository.isBusApiKeyConfigured()
        val subwayApiKey = settingsRepository.getSubwayApiKey()
        val isSubwayApiKeyOk = settingsRepository.isSubwayApiKeyConfigured()

        _settings.value = AdminSettingsState(
            homeAddressInput = home?.address ?: "",
            homeLatInput = home?.latitude?.toString() ?: "",
            homeLngInput = home?.longitude?.toString() ?: "",
            isHomeConfigured = home != null,
            destinations = dests,
            apiKeyInput = apiKey,
            isApiKeyConfigured = isApiKeyOk,
            busApiKeyInput = busApiKey,
            isBusApiKeyConfigured = isBusApiKeyOk,
            subwayApiKeyInput = subwayApiKey,
            isSubwayApiKeyConfigured = isSubwayApiKeyOk,
            apiCallCount = count
        )
    }

    // --- PIN Verification for Entering Admin Screen ---

    fun onPinDigitEntered(digit: String, onSuccess: () -> Unit) {
        if (_enteredPin.value.length < 4) {
            val updated = _enteredPin.value + digit
            _enteredPin.value = updated
            _pinError.value = false

            if (updated.length == 4) {
                val targetPin = settingsRepository.getPin()
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

    // --- Home Location Input Handling ---

    fun onHomeAddressChanged(address: String) {
        _settings.value = _settings.value.copy(
            homeAddressInput = address,
            homeAddressError = null,
            saveErrorMessage = null
        )
    }

    fun onHomeLatChanged(lat: String) {
        _settings.value = _settings.value.copy(
            homeLatInput = lat,
            homeLatError = null,
            saveErrorMessage = null
        )
    }

    fun onHomeLngChanged(lng: String) {
        _settings.value = _settings.value.copy(
            homeLngInput = lng,
            homeLngError = null,
            saveErrorMessage = null
        )
    }

    // --- API Key Handlers ---

    fun onApiKeyChanged(key: String) {
        _settings.value = _settings.value.copy(
            apiKeyInput = key,
            apiKeyError = null,
            saveErrorMessage = null
        )
    }

    fun onBusApiKeyChanged(key: String) {
        _settings.value = _settings.value.copy(
            busApiKeyInput = key,
            saveErrorMessage = null
        )
    }

    fun onSubwayApiKeyChanged(key: String) {
        _settings.value = _settings.value.copy(
            subwayApiKeyInput = key,
            saveErrorMessage = null
        )
    }

    // --- PIN Change Input Handling ---

    fun onNewPinChanged(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _settings.value = _settings.value.copy(
                newPinInput = pin,
                pinChangeError = null,
                saveErrorMessage = null
            )
        }
    }

    fun onConfirmPinChanged(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _settings.value = _settings.value.copy(
                confirmPinInput = pin,
                pinChangeError = null,
                saveErrorMessage = null
            )
        }
    }

    // --- Destination CRUD Handling ---

    fun openAddDestinationDialog() {
        if (_settings.value.destinations.size >= 6) {
            _settings.value = _settings.value.copy(
                destGeneralError = "목적지는 최대 6개까지만 등록할 수 있습니다."
            )
            return
        }
        _settings.value = _settings.value.copy(
            isDestDialogVisible = true,
            editingDestId = null,
            destShortNameInput = "",
            destFullNameInput = "",
            destLatInput = "",
            destLngInput = "",
            destShortNameError = null,
            destFullNameError = null,
            destLatError = null,
            destLngError = null,
            destGeneralError = null
        )
    }

    fun openEditDestinationDialog(destination: Destination) {
        _settings.value = _settings.value.copy(
            isDestDialogVisible = true,
            editingDestId = destination.id,
            destShortNameInput = destination.displayName,
            destFullNameInput = destination.name,
            destLatInput = destination.latitude.toString(),
            destLngInput = destination.longitude.toString(),
            destShortNameError = null,
            destFullNameError = null,
            destLatError = null,
            destLngError = null,
            destGeneralError = null
        )
    }

    fun closeDestinationDialog() {
        _settings.value = _settings.value.copy(
            isDestDialogVisible = false,
            editingDestId = null
        )
    }

    fun onDestShortNameChanged(name: String) {
        _settings.value = _settings.value.copy(
            destShortNameInput = name,
            destShortNameError = null
        )
    }

    fun onDestFullNameChanged(name: String) {
        _settings.value = _settings.value.copy(
            destFullNameInput = name,
            destFullNameError = null
        )
    }

    fun onDestLatChanged(lat: String) {
        _settings.value = _settings.value.copy(
            destLatInput = lat,
            destLatError = null
        )
    }

    fun onDestLngChanged(lng: String) {
        _settings.value = _settings.value.copy(
            destLngInput = lng,
            destLngError = null
        )
    }

    fun saveDestinationFromDialog(): Boolean {
        val state = _settings.value
        val shortName = state.destShortNameInput.trim()
        val fullName = state.destFullNameInput.trim()
        val latStr = state.destLatInput.trim()
        val lngStr = state.destLngInput.trim()

        var hasError = false
        var shortErr: String? = null
        var fullErr: String? = null
        var latErr: String? = null
        var lngErr: String? = null

        if (shortName.isBlank()) {
            shortErr = "표시할 짧은 이름을 입력해 주세요 (예: 병원)"
            hasError = true
        }

        if (fullName.isBlank()) {
            fullErr = "상세 명칭을 입력해 주세요 (예: 서울대학교병원)"
            hasError = true
        }

        val lat = latStr.toDoubleOrNull()
        if (latStr.isBlank() || lat == null || lat.isNaN() || lat.isInfinite() || lat < -90.0 || lat > 90.0) {
            latErr = "올바른 위도를 입력해 주세요 (-90 ~ 90)"
            hasError = true
        }

        val lng = lngStr.toDoubleOrNull()
        if (lngStr.isBlank() || lng == null || lng.isNaN() || lng.isInfinite() || lng < -180.0 || lng > 180.0) {
            lngErr = "올바른 경도를 입력해 주세요 (-180 ~ 180)"
            hasError = true
        }

        if (lat != null && lng != null && lat == 0.0 && lng == 0.0) {
            latErr = "0.0, 0.0 좌표는 등록할 수 없습니다."
            lngErr = "0.0, 0.0 좌표는 등록할 수 없습니다."
            hasError = true
        }

        if (hasError) {
            _settings.value = state.copy(
                destShortNameError = shortErr,
                destFullNameError = fullErr,
                destLatError = latErr,
                destLngError = lngErr
            )
            return false
        }

        val currentList = state.destinations.toMutableList()
        val editId = state.editingDestId

        if (editId != null) {
            val index = currentList.indexOfFirst { it.id == editId }
            if (index != -1) {
                val existing = currentList[index]
                val updated = existing.copy(
                    name = fullName,
                    displayName = shortName,
                    latitude = lat!!,
                    longitude = lng!!
                )
                currentList[index] = updated
                destinationRepository.saveDestination(updated)
            }
        } else {
            if (currentList.size >= 6) {
                _settings.value = state.copy(destGeneralError = "최대 6개까지만 등록할 수 있습니다.")
                return false
            }
            val newDest = Destination(
                id = UUID.randomUUID().toString(),
                name = fullName,
                displayName = shortName,
                latitude = lat!!,
                longitude = lng!!,
                icon = "location",
                order = currentList.size + 1
            )
            currentList.add(newDest)
            destinationRepository.saveDestination(newDest)
        }

        _settings.value = state.copy(
            destinations = currentList,
            isDestDialogVisible = false
        )
        return true
    }

    fun deleteDestination(id: String) {
        val updated = _settings.value.destinations.filterNot { it.id == id }
        val reordered = updated.mapIndexed { index, dest -> dest.copy(order = index + 1) }
        destinationRepository.updateDestinations(reordered)
        _settings.value = _settings.value.copy(destinations = reordered)
    }

    fun moveDestinationUp(id: String) {
        val current = _settings.value.destinations.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            val reordered = current.mapIndexed { i, dest -> dest.copy(order = i + 1) }
            destinationRepository.updateDestinations(reordered)
            _settings.value = _settings.value.copy(destinations = reordered)
        }
    }

    fun moveDestinationDown(id: String) {
        val current = _settings.value.destinations.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0 && index < current.size - 1) {
            val item = current.removeAt(index)
            current.add(index + 1, item)
            val reordered = current.mapIndexed { i, dest -> dest.copy(order = i + 1) }
            destinationRepository.updateDestinations(reordered)
            _settings.value = _settings.value.copy(destinations = reordered)
        }
    }

    // --- Save All Settings ---

    fun saveAllSettings(): Boolean {
        val state = _settings.value
        val addr = state.homeAddressInput.trim()
        val latStr = state.homeLatInput.trim()
        val lngStr = state.homeLngInput.trim()
        val newPin = state.newPinInput.trim()
        val confirmPin = state.confirmPinInput.trim()
        val apiKey = state.apiKeyInput.trim()
        val busApiKey = state.busApiKeyInput.trim()
        val subwayApiKey = state.subwayApiKeyInput.trim()

        var hasError = false
        var addrErr: String? = null
        var latErr: String? = null
        var lngErr: String? = null
        var pinErr: String? = null

        // 1. Home Location Validation
        val lat = latStr.toDoubleOrNull()
        val lng = lngStr.toDoubleOrNull()

        if (addr.isBlank()) {
            addrErr = "집 주소 또는 메모를 입력해 주세요."
            hasError = true
        } else if (addr == "집 위치 미설정") {
            addrErr = "실제 집 주소를 입력해 주세요."
            hasError = true
        }

        if (latStr.isBlank() || lat == null || lat.isNaN() || lat.isInfinite() || lat < -90.0 || lat > 90.0) {
            latErr = "올바른 위도를 입력해 주세요. (-90 ~ 90)"
            hasError = true
        }

        if (lngStr.isBlank() || lng == null || lng.isNaN() || lng.isInfinite() || lng < -180.0 || lng > 180.0) {
            lngErr = "올바른 경도를 입력해 주세요. (-180 ~ 180)"
            hasError = true
        }

        if (lat != null && lng != null && lat == 0.0 && lng == 0.0) {
            latErr = "0.0, 0.0 좌표는 등록할 수 없어요."
            lngErr = "0.0, 0.0 좌표는 등록할 수 없어요."
            hasError = true
        }

        // 2. PIN Change Validation
        if (newPin.isNotEmpty() || confirmPin.isNotEmpty()) {
            if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                pinErr = "새 PIN은 숫자 4자리여야 합니다."
                hasError = true
            } else if (newPin != confirmPin) {
                pinErr = "새 PIN과 확인 PIN이 일치하지 않습니다."
                hasError = true
            }
        }

        if (hasError) {
            _settings.value = state.copy(
                homeAddressError = addrErr,
                homeLatError = latErr,
                homeLngError = lngErr,
                pinChangeError = pinErr,
                saveErrorMessage = "입력값을 다시 확인해 주세요."
            )
            return false
        }

        // 3. Persist valid settings
        settingsRepository.saveHomeLocation(
            HomeLocation(latitude = lat!!, longitude = lng!!, address = addr)
        )

        if (newPin.isNotEmpty() && newPin == confirmPin) {
            settingsRepository.savePin(newPin)
        }

        if (apiKey.isNotEmpty()) {
            settingsRepository.saveApiKey(apiKey)
        }

        if (busApiKey.isNotEmpty()) {
            settingsRepository.saveBusApiKey(busApiKey)
        }

        if (subwayApiKey.isNotEmpty()) {
            settingsRepository.saveSubwayApiKey(subwayApiKey)
        }

        _settings.value = state.copy(
            isHomeConfigured = true,
            isApiKeyConfigured = settingsRepository.isApiKeyConfigured(),
            isBusApiKeyConfigured = settingsRepository.isBusApiKeyConfigured(),
            isSubwayApiKeyConfigured = settingsRepository.isSubwayApiKeyConfigured(),
            homeAddressError = null,
            homeLatError = null,
            homeLngError = null,
            newPinInput = "",
            confirmPinInput = "",
            pinChangeError = null,
            saveErrorMessage = null
        )
        return true
    }
}
