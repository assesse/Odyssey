package com.halmeoni.transit

import android.content.SharedPreferences

class TestSharedPreferences : SharedPreferences {
    private val dataMap = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = HashMap(dataMap)
    override fun getString(key: String, defValue: String?): String? = dataMap[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null
    override fun getInt(key: String, defValue: Int): Int = (dataMap[key] as? Number)?.toInt() ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (dataMap[key] as? Number)?.toLong() ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (dataMap[key] as? Number)?.toFloat() ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = dataMap[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = dataMap.containsKey(key)
    override fun edit(): SharedPreferences.Editor = TestEditor(dataMap)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class TestEditor(
        private val targetMap: MutableMap<String, Any>
    ) : SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            tempMap[key] = value
            return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            tempMap[key] = null
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }
        override fun commit(): Boolean {
            if (clearRequested) {
                targetMap.clear()
            }
            tempMap.forEach { (k, v) ->
                if (v == null) {
                    targetMap.remove(k)
                } else {
                    targetMap[k] = v
                }
            }
            tempMap.clear()
            clearRequested = false
            return true
        }
        override fun apply() {
            commit()
        }
    }
}
