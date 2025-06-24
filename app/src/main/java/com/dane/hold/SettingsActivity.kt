package com.dane.hold

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    private lateinit var holdDurationLayout: ConstraintLayout
    private lateinit var dailyGoalLayout: ConstraintLayout
    private lateinit var holdDurationValueText: TextView
    private lateinit var dailyGoalValueText: TextView
    private lateinit var overlaySwitch: SwitchMaterial
    private lateinit var newlyInstalledSwitch: SwitchMaterial
    private lateinit var toolbar: MaterialToolbar

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREFS_NAME = "AppSettings"
        const val KEY_HOLD_DURATION = "hold_duration"
        const val KEY_DAILY_GOAL = "daily_goal"
        const val KEY_OVERLAY = "overlay_enabled"
        const val KEY_NEWLY_INSTALLED = "newly_installed_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Find views from the layout
        toolbar = findViewById(R.id.toolbar_settings)
        holdDurationLayout = findViewById(R.id.layout_hold_duration)
        dailyGoalLayout = findViewById(R.id.layout_daily_goal)
        holdDurationValueText = findViewById(R.id.text_view_hold_duration_value)
        dailyGoalValueText = findViewById(R.id.text_view_daily_goal_value)
        overlaySwitch = findViewById(R.id.switch_overlay)
        newlyInstalledSwitch = findViewById(R.id.switch_newly_installed)

        // Setup the toolbar for navigation
        setupToolbar()

        // Load any saved settings into the UI
        loadSettings()

        // Setup listeners for user interactions
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            // Handle back button press
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadSettings() {
        // Load and display saved values, with defaults if they don't exist
        val holdDuration = sharedPreferences.getInt(KEY_HOLD_DURATION, 5) // Default 5 seconds
        holdDurationValueText.text = "$holdDuration sec"

        val dailyGoal = sharedPreferences.getInt(KEY_DAILY_GOAL, 10) // Default 10 minutes
        dailyGoalValueText.text = "$dailyGoal min"

        overlaySwitch.isChecked = sharedPreferences.getBoolean(KEY_OVERLAY, true)
        newlyInstalledSwitch.isChecked = sharedPreferences.getBoolean(KEY_NEWLY_INSTALLED, true)
    }

    private fun setupClickListeners() {
        holdDurationLayout.setOnClickListener {
            showValueDialog(
                "Default Hold Duration",
                "Enter duration in seconds",
                KEY_HOLD_DURATION,
                holdDurationValueText,
                "sec"
            )
        }

        dailyGoalLayout.setOnClickListener {
            showValueDialog(
                "Daily Goal",
                "Enter goal in minutes",
                KEY_DAILY_GOAL,
                dailyGoalValueText,
                "min"
            )
        }



        overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting(KEY_OVERLAY, isChecked)
        }

        newlyInstalledSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting(KEY_NEWLY_INSTALLED, isChecked)
        }
    }

    private fun showValueDialog(title: String, message: String, key: String, textView: TextView, unit: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)

        // Create an EditText for user input
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK") { dialog, _ ->
            val valueString = input.text.toString()
            if (valueString.isNotEmpty()) {
                val value = valueString.toInt()
                // Save the new value and update the UI
                saveIntSetting(key, value)
                textView.text = "$value $unit"
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun saveIntSetting(key: String, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply() // Use apply() for asynchronous saving
    }

    private fun saveBooleanSetting(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }
}