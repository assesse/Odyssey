package com.halmeoni.transit

import android.content.SharedPreferences
import com.halmeoni.transit.domain.ApiUsageTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiUsageTrackerTest {

    private class FakeSharedPreferences : SharedPreferences {
        private val stringMap = mutableMapOf<String, String>()
        private val intMap = mutableMapOf<String, Int>()

        override fun getAll(): MutableMap<String, *> = HashMap(stringMap + intMap)
        override fun getString(key: String, defValue: String?): String? = stringMap[key] ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String, defValue: Int): Int = intMap[key] ?: defValue
        override fun getLong(key: String, defValue: Long): Long = 0L
        override fun getFloat(key: String, defValue: Float): Float = 0f
        override fun getBoolean(key: String, defValue: Boolean): Boolean = false
        override fun contains(key: String): Boolean = stringMap.containsKey(key) || intMap.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(stringMap, intMap)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(
            private val stringMap: MutableMap<String, String>,
            private val intMap: MutableMap<String, Int>
        ) : SharedPreferences.Editor {
            private val tempString = mutableMapOf<String, String>()
            private val tempInt = mutableMapOf<String, Int>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                if (value != null) tempString[key] = value
                return this
            }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                tempInt[key] = value
                return this
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String): SharedPreferences.Editor = this
            override fun clear(): SharedPreferences.Editor = this
            override fun commit(): Boolean {
                stringMap.putAll(tempString)
                intMap.putAll(tempInt)
                return true
            }
            override fun apply() {
                commit()
            }
        }
    }

    private lateinit var fakePrefs: FakeSharedPreferences
    private var currentDate = "2026-08-25"

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        currentDate = "2026-08-25"
    }

    private fun createTracker(): ApiUsageTracker {
        return ApiUsageTracker(
            sharedPreferences = fakePrefs,
            dateSupplier = { currentDate }
        )
    }

    @Test
    fun `initial usage count is 0`() {
        val tracker = createTracker()
        assertEquals(0, tracker.getUsageCount())
        assertTrue(tracker.canMakeApiCall())
        assertFalse(tracker.isWarningThresholdReached())
        assertFalse(tracker.isLimitReached())
    }

    @Test
    fun `incrementUsage increases count and triggers warning and limit`() {
        val tracker = createTracker()

        for (i in 1..24) {
            val result = tracker.incrementUsage()
            assertEquals(ApiUsageTracker.UsageResult.Allowed, result)
        }
        assertEquals(24, tracker.getUsageCount())
        assertFalse(tracker.isWarningThresholdReached())

        val result25 = tracker.incrementUsage()
        assertEquals(ApiUsageTracker.UsageResult.Warning, result25)
        assertEquals(25, tracker.getUsageCount())
        assertTrue(tracker.isWarningThresholdReached())

        for (i in 26..29) {
            val res = tracker.incrementUsage()
            assertEquals(ApiUsageTracker.UsageResult.Warning, res)
        }
        assertEquals(29, tracker.getUsageCount())
        assertTrue(tracker.canMakeApiCall())

        val result30 = tracker.incrementUsage()
        assertEquals(ApiUsageTracker.UsageResult.LimitExceeded, result30)
        assertEquals(30, tracker.getUsageCount())
        assertFalse(tracker.canMakeApiCall())
        assertTrue(tracker.isLimitReached())

        val result31 = tracker.incrementUsage()
        assertEquals(ApiUsageTracker.UsageResult.LimitExceeded, result31)
        assertEquals(30, tracker.getUsageCount())
    }

    @Test
    fun `auto resets count when date changes`() {
        val tracker = createTracker()

        for (i in 1..30) {
            tracker.incrementUsage()
        }
        assertEquals(30, tracker.getUsageCount())
        assertTrue(tracker.isLimitReached())

        currentDate = "2026-08-26"

        assertEquals(0, tracker.getUsageCount())
        assertTrue(tracker.canMakeApiCall())
        assertFalse(tracker.isLimitReached())
        assertFalse(tracker.isWarningThresholdReached())
    }
}
