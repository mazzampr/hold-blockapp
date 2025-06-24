package com.dane.hold.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object LockedAppManager {
    private const val PREFS_NAME = "LockedAppsPrefs"
    private const val KEY_LOCKED_APPS = "locked_app_set"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addLockedApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val lockedApps = getLockedApps(context).toMutableSet()
        lockedApps.add(packageName)
        prefs.edit { putStringSet(KEY_LOCKED_APPS, lockedApps) }
    }

    fun removeLockedApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val lockedApps = getLockedApps(context).toMutableSet()
        lockedApps.remove(packageName)
        prefs.edit { putStringSet(KEY_LOCKED_APPS, lockedApps) }
    }

    fun getLockedApps(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_LOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun isAppLocked(context: Context, packageName: String): Boolean {
        return getLockedApps(context).contains(packageName)
    }
}