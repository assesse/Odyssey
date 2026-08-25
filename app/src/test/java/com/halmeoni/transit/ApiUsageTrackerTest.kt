package com.halmeoni.transit

import com.halmeoni.transit.domain.ApiUsageTracker
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ApiUsageTrackerTest {

    private lateinit var fakePrefs: TestSharedPreferences
    private var currentDate = "2026-08-25"

    @Before
    fun setUp() {
        fakePrefs = TestSharedPreferences()
        currentDate = "2026-08-25"
    }

    private fun createTracker(): ApiUsageTracker {
        return ApiUsageTracker(
            sharedPreferences = fakePrefs,
            dateSupplier = { currentDate }
        )
    }

    @Test
    fun initialUsageCountIs0() {
        val tracker = createTracker()
        assertEquals(0, tracker.getUsageCount())
    }

    @Test
    fun incrementUsageIncreasesCountWithoutHardBlocking() {
        val tracker = createTracker()

        for (i in 1..35) {
            val count = tracker.incrementUsage()
            assertEquals(i, count)
        }
        assertEquals(35, tracker.getUsageCount())
    }

    @Test
    fun autoResetsCountWhenDateChanges() {
        val tracker = createTracker()

        for (i in 1..30) {
            tracker.incrementUsage()
        }
        assertEquals(30, tracker.getUsageCount())

        currentDate = "2026-08-26"

        assertEquals(0, tracker.getUsageCount())
        val count = tracker.incrementUsage()
        assertEquals(1, count)
    }
}
