package com.halmeoni.transit

import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.model.HomeLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var testPrefs: TestSharedPreferences
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        testPrefs = TestSharedPreferences()
        repository = SettingsRepository(testPrefs)
    }

    @Test
    fun getHomeLocation_whenUnconfigured_returnsNull_andDoesNotFallbackToCityHall() {
        assertFalse(repository.hasHomeLocation())
        val home = repository.getHomeLocation()
        assertNull(home)
    }

    @Test
    fun saveHomeLocation_persistsAndRetrievesCorrectCoordinates() {
        val customHome = HomeLocation(latitude = 37.4500, longitude = 126.8500, address = "경기도 광명시")
        repository.saveHomeLocation(customHome)

        assertTrue(repository.hasHomeLocation())
        val retrieved = repository.getHomeLocation()
        assertNotNull(retrieved)
        assertEquals(37.4500, retrieved!!.latitude, 0.0001)
        assertEquals(126.8500, retrieved.longitude, 0.0001)
        assertEquals("경기도 광명시", retrieved.address)
    }

    @Test
    fun clearHomeLocation_removesHomeCoordinates() {
        val customHome = HomeLocation(latitude = 37.4500, longitude = 126.8500, address = "경기도 광명시")
        repository.saveHomeLocation(customHome)
        assertTrue(repository.hasHomeLocation())

        repository.clearHomeLocation()
        assertFalse(repository.hasHomeLocation())
        assertNull(repository.getHomeLocation())
    }

    @Test
    fun saveAndGetApiKey_persistsCustomApiKey() {
        assertFalse(repository.isApiKeyConfigured())
        repository.saveApiKey("TEST_ODSAY_KEY_12345")
        assertTrue(repository.isApiKeyConfigured())
        assertEquals("TEST_ODSAY_KEY_12345", repository.getApiKey())
    }
}
