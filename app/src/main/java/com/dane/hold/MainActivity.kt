package com.dane.hold

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

    private var fullAppList = mutableListOf<AppData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view_apps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        appAdapter = ListAppAdapter(mutableListOf(), this::checkPermissionsAndToggle, this::showSetDurationDialog)
        recyclerView.adapter = appAdapter

        // Get the list of installed apps and set up the adapter
        loadInitialApps()

        binding.btnSettings.setOnClickListener {
            navigateToSettings()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
        sortAndRefreshList()
    }

    private fun loadInitialApps() {
        val appList = mutableListOf<AppData>()
        val pm = packageManager
        val packages = pm.getInstalledApplications(0)
        for (appInfo in packages) {
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null && appInfo.packageName != this.packageName) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                val appIcon = pm.getApplicationIcon(appInfo)
                val packageName = appInfo.packageName
                appList.add(AppData(appName, appIcon, packageName))
            }
        }
        // Simpan ke variabel utama
        fullAppList.clear()
        fullAppList.addAll(appList)
        // Lakukan pengurutan pertama kali
        sortAndRefreshList()
    }

    private fun sortAndRefreshList() {
        val sortedList = fullAppList.sortedWith(
            compareBy<AppData> { !LockedAppManager.isAppLocked(this, it.packageName) }
                .thenBy { it.name.lowercase() }
        )
        appAdapter.updateFullList(sortedList)
    }

//    private fun getInstalledApps(): List<AppData> {
//        val appList = mutableListOf<AppData>()
//        val pm = packageManager
//        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
//
//        for (appInfo in packages) {
//            // Filter out system apps and only get apps that can be launched
//            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
//                val appName = pm.getApplicationLabel(appInfo).toString()
//                val appIcon = pm.getApplicationIcon(appInfo)
//                val packageName = appInfo.packageName
//                appList.add(AppData(appName, appIcon, packageName))
//            }
//        }
//
//        // Sort the list alphabetically by app name for better UX
//        appList.sortBy { it.name.lowercase() }
//
//        return appList
//    }

    private fun checkPermissionsAndToggle(packageName: String, isChecked: Boolean, callback: (success: Boolean) -> Unit) {
        if (isChecked) {
            if (areAllPermissionsGranted()) {
                val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val defaultDuration = prefs.getInt(SettingsActivity.KEY_HOLD_DURATION, 5)
                LockedAppManager.setLockedAppDuration(this, packageName, defaultDuration)
                sortAndRefreshList()
                callback(true)
            } else {
                startActivity(Intent(this, PermissionActivity::class.java))
                callback(false)
            }
        } else {
            LockedAppManager.removeLockedApp(this, packageName)
            sortAndRefreshList()
            callback(true)
        }
    }

    /**
     * Fungsi BARU untuk menampilkan dialog pengaturan durasi kustom.
     */
    private fun showSetDurationDialog(packageName: String, currentDuration: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Custom Duration")
        builder.setMessage("Enter new hold duration in seconds for this app.")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentDuration.toString())
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val valueString = input.text.toString()
            if (valueString.isNotEmpty()) {
                val newDuration = valueString.toInt()
                LockedAppManager.setLockedAppDuration(this, packageName, newDuration)
                sortAndRefreshList() // Muat ulang daftar untuk menampilkan nilai baru
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
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