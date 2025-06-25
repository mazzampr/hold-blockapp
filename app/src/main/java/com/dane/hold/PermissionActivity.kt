package com.dane.hold

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class PermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        // Find all cards from layout
        val backgroundCard: MaterialCardView = findViewById(R.id.card_background_permission)
        val overlayCard: MaterialCardView = findViewById(R.id.card_overlay_permission)
        val accessibilityCard: MaterialCardView = findViewById(R.id.card_accessibility_permission)
        val btnClose: ImageView = findViewById(R.id.btn_close)

        backgroundCard.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }
        overlayCard.setOnClickListener {
            requestOverlayPermission()
        }
        accessibilityCard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btnClose.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isIgnoringBatteryOptimizations() && hasOverlayPermission() && isAccessibilityServiceEnabled()) {
            finish()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
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
        } else true
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val canonicalName = AppLockerService::class.java.canonicalName ?: return false
        val service = "$packageName/$canonicalName"
        val settingValue = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return settingValue?.let {
            val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(it) }
            (splitter as Iterable<String>).asSequence().any { it.equals(service, ignoreCase = true) }
        } ?: false
    }
}