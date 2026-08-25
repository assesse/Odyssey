package com.halmeoni.transit

import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.model.Destination
import com.halmeoni.transit.domain.model.HomeLocation
import com.halmeoni.transit.ui.admin.AdminViewModel
import com.halmeoni.transit.ui.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminViewModelTest {

    private lateinit var testPrefs: TestSharedPreferences
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var destRepo: DestinationRepository
    private lateinit var tracker: ApiUsageTracker
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        testPrefs = TestSharedPreferences()
        settingsRepo = SettingsRepository(testPrefs)
        destRepo = DestinationRepository(testPrefs)
        tracker = ApiUsageTracker(testPrefs)
        viewModel = AdminViewModel(settingsRepo, destRepo, tracker)
    }

    // --- Home Location Validation Tests ---

    @Test
    fun saveValidHomeLocation_success() {
        viewModel.onHomeAddressChanged("서울시 마포구 공덕동")
        viewModel.onHomeLatChanged("37.5445")
        viewModel.onHomeLngChanged("126.9512")

        val result = viewModel.saveAllSettings()
        assertTrue(result)

        val saved = settingsRepo.getHomeLocation()
        assertNotNull(saved)
        assertEquals("서울시 마포구 공덕동", saved?.address)
        assertEquals(37.5445, saved!!.latitude, 0.0001)
        assertEquals(126.9512, saved.longitude, 0.0001)
        assertTrue(viewModel.settings.value.isHomeConfigured)
    }

    @Test
    fun saveHomeLocation_rejectsInvalidLatitude() {
        viewModel.onHomeAddressChanged("우리집")
        viewModel.onHomeLatChanged("95.0") // Over 90
        viewModel.onHomeLngChanged("127.0")

        val result = viewModel.saveAllSettings()
        assertFalse(result)
        assertNull(settingsRepo.getHomeLocation())
        assertNotNull(viewModel.settings.value.homeLatError)
    }

    @Test
    fun saveHomeLocation_rejectsInvalidLongitude() {
        viewModel.onHomeAddressChanged("우리집")
        viewModel.onHomeLatChanged("37.5")
        viewModel.onHomeLngChanged("185.0") // Over 180

        val result = viewModel.saveAllSettings()
        assertFalse(result)
        assertNull(settingsRepo.getHomeLocation())
        assertNotNull(viewModel.settings.value.homeLngError)
    }

    @Test
    fun saveHomeLocation_rejectsNonNumericAndEmptyInputs() {
        viewModel.onHomeAddressChanged("우리집")
        viewModel.onHomeLatChanged("abc")
        viewModel.onHomeLngChanged("")

        val result = viewModel.saveAllSettings()
        assertFalse(result)
        assertNull(settingsRepo.getHomeLocation())
    }

    @Test
    fun saveHomeLocation_rejectsZeroZeroSentinelCoordinates() {
        viewModel.onHomeAddressChanged("우리집")
        viewModel.onHomeLatChanged("0.0")
        viewModel.onHomeLngChanged("0.0")

        val result = viewModel.saveAllSettings()
        assertFalse(result)
        assertNull(settingsRepo.getHomeLocation())
        assertNotNull(viewModel.settings.value.homeLatError)
    }

    @Test
    fun saveHomeLocation_preservesExistingDataOnValidationFailure() {
        val existing = HomeLocation(37.5, 127.0, "원래 집")
        settingsRepo.saveHomeLocation(existing)

        // Try invalid update
        viewModel.onHomeAddressChanged("새 집")
        viewModel.onHomeLatChanged("invalid")
        viewModel.onHomeLngChanged("127.0")

        val result = viewModel.saveAllSettings()
        assertFalse(result)

        val current = settingsRepo.getHomeLocation()
        assertEquals("원래 집", current?.address)
        assertEquals(37.5, current!!.latitude, 0.0001)
    }

    // --- Destination CRUD Tests ---

    @Test
    fun destination_add_update_delete_andOrder() {
        // 1. Add destination
        viewModel.openAddDestinationDialog()
        viewModel.onDestShortNameChanged("병원")
        viewModel.onDestFullNameChanged("서울대학교병원")
        viewModel.onDestLatChanged("37.5796")
        viewModel.onDestLngChanged("126.9990")

        val addResult = viewModel.saveDestinationFromDialog()
        assertTrue(addResult)
        assertEquals(1, destRepo.getDestinations().size)
        val added = destRepo.getDestinations()[0]
        assertEquals("병원", added.displayName)
        assertEquals("서울대학교병원", added.name)
        assertEquals(1, added.order)

        // 2. Add second destination
        viewModel.openAddDestinationDialog()
        viewModel.onDestShortNameChanged("시장")
        viewModel.onDestFullNameChanged("경동시장")
        viewModel.onDestLatChanged("37.5804")
        viewModel.onDestLngChanged("127.0385")
        viewModel.saveDestinationFromDialog()

        assertEquals(2, destRepo.getDestinations().size)

        // 3. Move order
        viewModel.moveDestinationUp(destRepo.getDestinations()[1].id)
        val reordered = destRepo.getDestinations()
        assertEquals("시장", reordered[0].displayName)
        assertEquals("병원", reordered[1].displayName)

        // 4. Edit destination (preserves ID)
        val destToEdit = reordered[0]
        viewModel.openEditDestinationDialog(destToEdit)
        viewModel.onDestShortNameChanged("전통시장")
        viewModel.saveDestinationFromDialog()

        val updated = destRepo.getDestinationById(destToEdit.id)
        assertNotNull(updated)
        assertEquals("전통시장", updated?.displayName)
        assertEquals(destToEdit.id, updated?.id)

        // 5. Delete destination
        viewModel.deleteDestination(destToEdit.id)
        assertEquals(1, destRepo.getDestinations().size)
    }

    @Test
    fun destination_enforcesMaximumLimitOf6() {
        for (i in 1..6) {
            viewModel.openAddDestinationDialog()
            viewModel.onDestShortNameChanged("장소$i")
            viewModel.onDestFullNameChanged("전체이름$i")
            viewModel.onDestLatChanged("37.5$i")
            viewModel.onDestLngChanged("127.0$i")
            assertTrue(viewModel.saveDestinationFromDialog())
        }
        assertEquals(6, destRepo.getDestinations().size)

        // 7th attempt should be blocked
        viewModel.openAddDestinationDialog()
        assertNotNull(viewModel.settings.value.destGeneralError)
    }

    // --- PIN Validation Tests ---

    @Test
    fun pin_changeValidation() {
        // Default PIN is 1234
        assertEquals("1234", settingsRepo.getPin())

        // 1. Success change to 5678
        viewModel.onHomeAddressChanged("집")
        viewModel.onHomeLatChanged("37.5")
        viewModel.onHomeLngChanged("127.0")
        viewModel.onNewPinChanged("5678")
        viewModel.onConfirmPinChanged("5678")
        assertTrue(viewModel.saveAllSettings())
        assertEquals("5678", settingsRepo.getPin())

        // 2. Mismatch confirmation
        viewModel.onNewPinChanged("9999")
        viewModel.onConfirmPinChanged("8888")
        assertFalse(viewModel.saveAllSettings())
        assertEquals("5678", settingsRepo.getPin()) // Preserved

        // 3. Short PIN
        viewModel.onNewPinChanged("12")
        viewModel.onConfirmPinChanged("12")
        assertFalse(viewModel.saveAllSettings())
        assertEquals("5678", settingsRepo.getPin()) // Preserved
    }

    // --- Home Screen Refresh Test ---

    @Test
    fun homeViewModel_displaysNewlyAddedDestinationAfterAdminSave() {
        val homeViewModel = HomeViewModel(destRepo)
        assertEquals(0, homeViewModel.uiState.value.destinations.size)

        // Add destination via Admin
        viewModel.openAddDestinationDialog()
        viewModel.onDestShortNameChanged("복지관")
        viewModel.onDestFullNameChanged("종로노인복지관")
        viewModel.onDestLatChanged("37.5760")
        viewModel.onDestLngChanged("126.9980")
        viewModel.saveDestinationFromDialog()

        // Home screen reloads
        homeViewModel.loadDestinations()
        assertEquals(1, homeViewModel.uiState.value.destinations.size)
        assertEquals("복지관", homeViewModel.uiState.value.destinations[0].displayName)
    }
}
