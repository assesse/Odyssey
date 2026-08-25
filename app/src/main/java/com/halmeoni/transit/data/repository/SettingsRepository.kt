package com.halmeoni.transit.data.repository

import android.content.SharedPreferences
import com.halmeoni.transit.BuildConfig
import com.halmeoni.transit.domain.model.HomeLocation

class SettingsRepository(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val PREF_KEY_PIN = "admin_pin"
        private const val PREF_KEY_HOME_LAT = "home_latitude"
        private const val PREF_KEY_HOME_LNG = "home_longitude"
        private const val PREF_KEY_HOME_ADDR = "home_address"
        private const val PREF_KEY_CUSTOM_API_KEY = "custom_odsay_api_key"

        const val DEFAULT_PIN = "1234"
    }

    fun getPin(): String {
        return sharedPreferences.getString(PREF_KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun savePin(pin: String) {
        sharedPreferences.edit().putString(PREF_KEY_PIN, pin).apply()
    }

    fun verifyPin(inputPin: String): Boolean {
        return getPin() == inputPin
    }

    fun hasHomeLocation(): Boolean {
        return sharedPreferences.contains(PREF_KEY_HOME_LAT) &&
                sharedPreferences.contains(PREF_KEY_HOME_LNG)
    }

    fun getHomeLocation(): HomeLocation? {
        if (!hasHomeLocation()) {
            return null
        }
        val lat = sharedPreferences.getFloat(PREF_KEY_HOME_LAT, 0f).toDouble()
        val lng = sharedPreferences.getFloat(PREF_KEY_HOME_LNG, 0f).toDouble()
        val addr = sharedPreferences.getString(PREF_KEY_HOME_ADDR, "") ?: ""
        return HomeLocation(latitude = lat, longitude = lng, address = addr)
    }

    fun saveHomeLocation(homeLocation: HomeLocation) {
        sharedPreferences.edit()
            .putFloat(PREF_KEY_HOME_LAT, homeLocation.latitude.toFloat())
            .putFloat(PREF_KEY_HOME_LNG, homeLocation.longitude.toFloat())
            .putString(PREF_KEY_HOME_ADDR, homeLocation.address)
            .apply()
    }

    fun clearHomeLocation() {
        sharedPreferences.edit()
            .remove(PREF_KEY_HOME_LAT)
            .remove(PREF_KEY_HOME_LNG)
            .remove(PREF_KEY_HOME_ADDR)
            .apply()
    }

    fun getApiKey(): String {
        val customKey = sharedPreferences.getString(PREF_KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
        if (customKey.isNotBlank() && customKey != "PLACEHOLDER_KEY" && customKey != "PLACEHOLDER_ODSAY_API_KEY") {
            return customKey
        }
        val buildKey = BuildConfig.ODSAY_API_KEY.trim()
        return if (buildKey != "PLACEHOLDER_KEY" && buildKey != "PLACEHOLDER_ODSAY_API_KEY") buildKey else ""
    }

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(PREF_KEY_CUSTOM_API_KEY, apiKey.trim())
            .apply()
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }
}
