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
        const val DEFAULT_LAT = 37.5665
        const val DEFAULT_LNG = 126.9780
        const val DEFAULT_ADDR = "서울특별시 중구 세종대로 110"
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

    fun getHomeLocation(): HomeLocation {
        val lat = sharedPreferences.getFloat(PREF_KEY_HOME_LAT, DEFAULT_LAT.toFloat()).toDouble()
        val lng = sharedPreferences.getFloat(PREF_KEY_HOME_LNG, DEFAULT_LNG.toFloat()).toDouble()
        val addr = sharedPreferences.getString(PREF_KEY_HOME_ADDR, DEFAULT_ADDR) ?: DEFAULT_ADDR
        return HomeLocation(latitude = lat, longitude = lng, address = addr)
    }

    fun saveHomeLocation(homeLocation: HomeLocation) {
        sharedPreferences.edit()
            .putFloat(PREF_KEY_HOME_LAT, homeLocation.latitude.toFloat())
            .putFloat(PREF_KEY_HOME_LNG, homeLocation.longitude.toFloat())
            .putString(PREF_KEY_HOME_ADDR, homeLocation.address)
            .apply()
    }
}
