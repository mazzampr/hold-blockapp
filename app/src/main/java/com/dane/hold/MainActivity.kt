package com.dane.hold

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dane.hold.adapter.ListAppAdapter
import com.dane.hold.data.AppData
import com.dane.hold.data.LockedAppManager
import com.dane.hold.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: ListAppAdapter
    private lateinit var searchEditText: EditText
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view_apps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get the list of installed apps and set up the adapter
        val installedApps = getInstalledApps()
        appAdapter = ListAppAdapter(installedApps, this::checkPermissionsAndToggle)
        recyclerView.adapter = appAdapter

        binding.btnSettings.setOnClickListener {
            navigateToSettings()
        }

        searchEditText = findViewById(R.id.edit_text_search)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the app list based on the search input
                appAdapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {
                // Nothing to do here
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Refresh the adapter's UI when returning to this screen
        if (::appAdapter.isInitialized) {
            appAdapter.notifyDataSetChanged()
        }
    }

    private fun getInstalledApps(): List<AppData> {
        val appList = mutableListOf<AppData>()
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            // Filter out system apps and only get apps that can be launched
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                val appIcon = pm.getApplicationIcon(appInfo)
                val packageName = appInfo.packageName
                appList.add(AppData(appName, appIcon, packageName))
            }
        }

        // Sort the list alphabetically by app name for better UX
        appList.sortBy { it.name.lowercase() }

        return appList
    }

    private fun checkPermissionsAndToggle(packageName: String, isChecked: Boolean, callback: (success: Boolean) -> Unit) {
        if (isChecked) {
            // User wants to LOCK an app
            if (areAllPermissionsGranted()) {
                // All permissions are granted, proceed to lock
                LockedAppManager.addLockedApp(this, packageName)
                callback(true) // Signal success to the adapter
            } else {
                // One or more permissions are missing, show the permissions screen
                startActivity(Intent(this, PermissionActivity::class.java))
                callback(false) // Signal failure, the adapter will reset the toggle
            }
        } else {
            // User wants to UNLOCK an app, no permission needed for this
            LockedAppManager.removeLockedApp(this, packageName)
            callback(true) // Unlocking always succeeds
        }
    }

    private fun areAllPermissionsGranted(): Boolean {
        return isIgnoringBatteryOptimizations() && hasOverlayPermission() && isAccessibilityServiceEnabled()
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    /**
     * This specifically checks if our AppLockerService is enabled in Accessibility settings.
     * This is a crucial check.
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val canonicalName = AppLockerService::class.java.canonicalName ?: return false
        val service = "$packageName/$canonicalName"
        val settingValue = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return settingValue?.let {
            val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(it) }
            (splitter as Iterable<String>).asSequence().any { it.equals(service, ignoreCase = true) }
        } ?: false
    }

    private fun navigateToSettings() {
        // Navigate to SettingsActivity
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}