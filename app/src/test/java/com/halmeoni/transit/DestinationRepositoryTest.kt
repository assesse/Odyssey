package com.halmeoni.transit

import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.domain.model.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DestinationRepositoryTest {

    private lateinit var testPrefs: TestSharedPreferences
    private lateinit var repository: DestinationRepository

    @Before
    fun setUp() {
        testPrefs = TestSharedPreferences()
        repository = DestinationRepository(testPrefs)
    }

    @Test
    fun getDestinations_whenEmpty_returnsEmptyList_andDoesNotAutoInjectDefaults() {
        val list = repository.getDestinations()
        assertTrue(list.isEmpty())
        assertFalse(repository.hasDestinations())
    }

    @Test
    fun getDestinationById_returnsMatchingDestinationOrNull() {
        val dest = Destination(
            id = "hospital_1",
            name = "서울대학교병원",
            displayName = "병원",
            latitude = 37.5796,
            longitude = 126.9990,
            icon = "hospital",
            order = 1
        )
        repository.saveDestination(dest)

        val found = repository.getDestinationById("hospital_1")
        assertNotNull(found)
        assertEquals("병원", found?.displayName)

        val notFound = repository.getDestinationById("non_existent_id")
        assertNull(notFound)
    }

    @Test
    fun resetToDefaults_explicitlyPopulatesDefaultDestinations() {
        val list = repository.resetToDefaults()
        assertEquals(4, list.size)
        assertTrue(repository.hasDestinations())
        assertNotNull(repository.getDestinationById("default_hospital"))
    }
}
