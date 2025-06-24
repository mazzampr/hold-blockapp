package com.dane.hold

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.dane.hold.data.LockedAppManager

class AppLockerService: AccessibilityService() {

    private var appInstallReceiver: AppInstallReceiver? = null

    companion object {
        // Variabel untuk "mengingat" aplikasi yang baru saja berhasil dibuka
        var lastUnlockedAppPackage: String? = null
        // Flag untuk mencegah beberapa overlay muncul secara bersamaan
        var isOverlayShowing = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        appInstallReceiver = AppInstallReceiver()
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        registerReceiver(appInstallReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName != lastUnlockedAppPackage && !isLauncher(packageName)) {
                lastUnlockedAppPackage = null
            }

//            if (isLauncher(packageName)) {
//                lastUnlockedAppPackage = null
//            }

            val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val isOverlayEnabled = prefs.getBoolean(SettingsActivity.KEY_OVERLAY, true)

            val isAppLocked = LockedAppManager.isAppLocked(this, packageName)
            val shouldLock = isAppLocked && packageName != lastUnlockedAppPackage && isOverlayEnabled

            // Tampilkan overlay HANYA jika diperlukan DAN tidak sedang tampil.
            if (shouldLock && !isOverlayShowing) {
                // Set flag ini SEGERA untuk memblokir pemicu lain
                isOverlayShowing = true

                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    "this app"
                }

                val intent = Intent(this, OverlayService::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("APP_NAME", appName)
                    putExtra("PACKAGE_NAME", packageName)
                }
                startService(intent)
            }
        }
    }

    /**
     * Helper function to check if a package is the default launcher.
     */
    private fun isLauncher(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver untuk mencegah memory leak
        if (appInstallReceiver != null) {
            unregisterReceiver(appInstallReceiver)
        }
    }

    override fun onInterrupt() {
        // This method is called when the service is interrupted
    }
}