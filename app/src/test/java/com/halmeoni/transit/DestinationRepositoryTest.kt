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
    fun saveAndDeleteDestination_updatesListCorrectly() {
        val dest1 = Destination("id_1", "장소 1", "표시 1", 37.5, 127.0, "place", 1)
        val dest2 = Destination("id_2", "장소 2", "표시 2", 37.6, 127.1, "place", 2)

        repository.saveDestination(dest1)
        repository.saveDestination(dest2)
        assertEquals(2, repository.getDestinations().size)

        repository.deleteDestination("id_1")
        val remaining = repository.getDestinations()
        assertEquals(1, remaining.size)
        assertEquals("id_2", remaining[0].id)
    }

    @Test
    fun updateDestinations_persistsAcrossRepositoryRecreation() {
        val destList = listOf(
            Destination("id_1", "장소 1", "표시 1", 37.5, 127.0, "place", 1),
            Destination("id_2", "장소 2", "표시 2", 37.6, 127.1, "place", 2)
        )
        repository.updateDestinations(destList)

        // Simulate app restart by creating a new repository with the same SharedPreferences
        val recreatedRepo = DestinationRepository(testPrefs)
        val loaded = recreatedRepo.getDestinations()
        assertEquals(2, loaded.size)
        assertEquals("id_1", loaded[0].id)
        assertEquals("id_2", loaded[1].id)
    }
}
