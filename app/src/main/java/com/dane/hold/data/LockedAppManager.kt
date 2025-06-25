package com.dane.hold.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object LockedAppManager {
    private const val PREFS_NAME = "LockedAppsPrefs"
    private const val KEY_LOCKED_APPS_MAP = "locked_apps_map"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Sets or updates the custom duration for a specific app.
     */
    fun setLockedAppDuration(context: Context, packageName: String, duration: Int) {
        val prefs = getPrefs(context)
        val map = getLockedAppsMap(context)
        map[packageName] = duration
        saveMap(prefs, map)
    }

    /**
     * Removes an app from the locked list.
     */
    fun removeLockedApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val map = getLockedAppsMap(context)
        map.remove(packageName)
        saveMap(prefs, map)
    }

    /**
     * Gets the custom duration for a specific app.
     * @return The duration in seconds, or -1 if no custom duration is set.
     */
    fun getLockedAppDuration(context: Context, packageName: String): Int {
        val map = getLockedAppsMap(context)
        return map.getOrElse(packageName) { -1 }
    }

    /**
     * Checks if an app is currently locked.
     * An app is considered locked if it has a custom duration set.
     */
    fun isAppLocked(context: Context, packageName: String): Boolean {
        return getLockedAppsMap(context).containsKey(packageName)
    }

    /**
     * Helper to retrieve the entire map from JSON storage.
     */
    private fun getLockedAppsMap(context: Context): MutableMap<String, Int> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_LOCKED_APPS_MAP, null)
        val map = mutableMapOf<String, Int>()

        if (jsonString != null) {
            val json = JSONObject(jsonString)
            for (key in json.keys()) {
                map[key] = json.getInt(key)
            }
        }
        return map
    }

    /**
     * Helper to save the map as a JSON string.
     */
    private fun saveMap(prefs: SharedPreferences, map: Map<String, Int>) {
        val json = JSONObject()
        for ((key, value) in map) {
            json.put(key, value)
        }
        prefs.edit().putString(KEY_LOCKED_APPS_MAP, json.toString()).apply()
    }
}