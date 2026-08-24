package com.halmeoni.transit.data.repository

import android.content.SharedPreferences
import com.halmeoni.transit.domain.model.HomeLocation

class SettingsRepository(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val PREF_KEY_PIN = "admin_pin"
        private const val PREF_KEY_HOME_LAT = "home_latitude"
        private const val PREF_KEY_HOME_LNG = "home_longitude"
        private const val PREF_KEY_HOME_ADDR = "home_address"

        const val DEFAULT_PIN = "0000"
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
}
