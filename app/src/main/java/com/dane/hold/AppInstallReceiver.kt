package com.dane.hold

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dane.hold.data.LockedAppManager

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val shouldLockNewApps = prefs.getBoolean(SettingsActivity.KEY_NEWLY_INSTALLED, true)

            if (shouldLockNewApps) {
                val newPackageName = intent.data?.schemeSpecificPart
                if (newPackageName != null) {
                    val defaultDuration = prefs.getInt(SettingsActivity.KEY_HOLD_DURATION, 5)
                    LockedAppManager.setLockedAppDuration(context, newPackageName, defaultDuration)
                }
            }
        }
    }
}