package com.halmeoni.transit.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.halmeoni.transit.domain.model.Destination

class DestinationRepository(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson = Gson()
) {
    companion object {
        private const val PREF_KEY_DESTINATIONS = "destinations_list"

        val DEFAULT_DESTINATIONS = listOf(
            Destination(
                id = "default_hospital",
                name = "서울대학교병원",
                displayName = "병원",
                latitude = 37.5796,
                longitude = 126.9990,
                icon = "hospital",
                order = 1
            ),
            Destination(
                id = "default_market",
                name = "경동시장",
                displayName = "시장",
                latitude = 37.5804,
                longitude = 127.0385,
                icon = "market",
                order = 2
            ),
            Destination(
                id = "default_welfare",
                name = "종로노인복지관",
                displayName = "복지관",
                latitude = 37.5760,
                longitude = 126.9980,
                icon = "welfare",
                order = 3
            ),
            Destination(
                id = "default_park",
                name = "탑골공원",
                displayName = "공원",
                latitude = 37.5712,
                longitude = 126.9882,
                icon = "park",
                order = 4
            )
        )
    }

    fun getDestinations(): List<Destination> {
        val json = sharedPreferences.getString(PREF_KEY_DESTINATIONS, null)
        if (json.isNullOrEmpty()) {
            return emptyList()
        }
        val type = object : TypeToken<List<Destination>>() {}.type
        val list: List<Destination>? = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
        return list?.sortedBy { it.order } ?: emptyList()
    }

    fun getDestinationById(id: String): Destination? {
        return getDestinations().firstOrNull { it.id == id }
    }

    fun hasDestinations(): Boolean {
        return getDestinations().isNotEmpty()
    }

    fun saveDestination(destination: Destination) {
        val current = getDestinations().toMutableList()
        val index = current.indexOfFirst { it.id == destination.id }
        if (index >= 0) {
            current[index] = destination
        } else {
            current.add(destination)
        }
        saveAll(current)
    }

    fun deleteDestination(id: String) {
        val current = getDestinations().filterNot { it.id == id }
        saveAll(current)
    }

    fun updateDestinationOrder(destinations: List<Destination>) {
        saveAll(destinations)
    }

    fun resetToDefaults(): List<Destination> {
        saveAll(DEFAULT_DESTINATIONS)
        return DEFAULT_DESTINATIONS
    }

    private fun saveAll(list: List<Destination>) {
        val sorted = list.sortedBy { it.order }
        val json = gson.toJson(sorted)
        sharedPreferences.edit().putString(PREF_KEY_DESTINATIONS, json).apply()
    }
}
