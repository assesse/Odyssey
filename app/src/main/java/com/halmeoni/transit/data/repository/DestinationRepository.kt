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

    fun updateDestinations(destinations: List<Destination>) {
        saveAll(destinations)
    }

    private fun saveAll(list: List<Destination>) {
        val sorted = list.sortedBy { it.order }
        val json = gson.toJson(sorted)
        sharedPreferences.edit().putString(PREF_KEY_DESTINATIONS, json).apply()
    }
}
