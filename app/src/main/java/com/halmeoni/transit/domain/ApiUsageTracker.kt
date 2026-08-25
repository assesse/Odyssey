package com.halmeoni.transit.domain

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApiUsageTracker(
    private val sharedPreferences: SharedPreferences,
    private val dateSupplier: () -> String = {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
) {
    companion object {
        private const val PREF_KEY_DATE = "api_usage_date"
        private const val PREF_KEY_COUNT = "api_usage_count"
    }

    @Synchronized
    fun getUsageCount(): Int {
        checkAndResetIfNewDay()
        return sharedPreferences.getInt(PREF_KEY_COUNT, 0)
    }

    @Synchronized
    fun incrementUsage(): Int {
        checkAndResetIfNewDay()
        val currentCount = sharedPreferences.getInt(PREF_KEY_COUNT, 0)
        val newCount = currentCount + 1
        sharedPreferences.edit()
            .putInt(PREF_KEY_COUNT, newCount)
            .putString(PREF_KEY_DATE, dateSupplier())
            .apply()
        return newCount
    }

    @Synchronized
    private fun checkAndResetIfNewDay() {
        val today = dateSupplier()
        val savedDate = sharedPreferences.getString(PREF_KEY_DATE, null)
        if (savedDate != today) {
            sharedPreferences.edit()
                .putString(PREF_KEY_DATE, today)
                .putInt(PREF_KEY_COUNT, 0)
                .apply()
        }
    }
}
