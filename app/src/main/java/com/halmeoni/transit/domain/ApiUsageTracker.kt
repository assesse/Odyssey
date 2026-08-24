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
        const val PREF_KEY_DATE = "api_usage_date"
        const val PREF_KEY_COUNT = "api_usage_count"
        const val MAX_DAILY_CALLS = 30
        const val WARNING_THRESHOLD = 25
    }

    sealed class UsageResult {
        object Allowed : UsageResult()
        object Warning : UsageResult()
        object LimitExceeded : UsageResult()
    }

    private fun checkAndResetIfNewDay() {
        val today = dateSupplier()
        val savedDate = sharedPreferences.getString(PREF_KEY_DATE, "") ?: ""
        if (savedDate != today) {
            sharedPreferences.edit()
                .putString(PREF_KEY_DATE, today)
                .putInt(PREF_KEY_COUNT, 0)
                .apply()
        }
    }

    fun getUsageCount(): Int {
        checkAndResetIfNewDay()
        return sharedPreferences.getInt(PREF_KEY_COUNT, 0)
    }

    fun canMakeApiCall(): Boolean {
        return getUsageCount() < MAX_DAILY_CALLS
    }

    fun isWarningThresholdReached(): Boolean {
        return getUsageCount() >= WARNING_THRESHOLD
    }

    fun isLimitReached(): Boolean {
        return getUsageCount() >= MAX_DAILY_CALLS
    }

    fun incrementUsage(): UsageResult {
        checkAndResetIfNewDay()
        val currentCount = sharedPreferences.getInt(PREF_KEY_COUNT, 0)
        if (currentCount >= MAX_DAILY_CALLS) {
            return UsageResult.LimitExceeded
        }

        val newCount = currentCount + 1
        sharedPreferences.edit()
            .putInt(PREF_KEY_COUNT, newCount)
            .apply()

        return when {
            newCount >= MAX_DAILY_CALLS -> UsageResult.LimitExceeded
            newCount >= WARNING_THRESHOLD -> UsageResult.Warning
            else -> UsageResult.Allowed
        }
    }
}
