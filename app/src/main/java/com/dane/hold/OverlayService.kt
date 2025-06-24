package com.dane.hold

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class OverlayService: Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var countDownTimer: CountDownTimer? = null
    private var currentPackageName: String? = null
    private var unlockSuccessful = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra("APP_NAME") ?: "this app"
        currentPackageName = intent?.getStringExtra("PACKAGE_NAME")

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            0,
            PixelFormat.TRANSLUCENT
        )

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        overlayView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        overlayView.isFocusableInTouchMode = true
        overlayView.requestFocus()
        overlayView.setOnKeyListener { _, keyCode, _ ->
            keyCode == KeyEvent.KEYCODE_BACK
        }
        overlayView.setOnKeyListener { _, keyCode, _ ->
            keyCode == KeyEvent.KEYCODE_HOME
        }
        overlayView.setOnKeyListener { _, keyCode, _ ->
            keyCode == KeyEvent.KEYCODE_MENU
        }

        val titleText: TextView = overlayView.findViewById(R.id.text_overlay_title)
        val countdownText: TextView = overlayView.findViewById(R.id.text_countdown)
        val progressBar: ProgressBar = overlayView.findViewById(R.id.progress_bar_countdown)
        val holdButton: Button = overlayView.findViewById(R.id.button_hold)
        val btnExit: Button = overlayView.findViewById(R.id.btn_exit)

        val dailyGoalText: TextView = overlayView.findViewById(R.id.text_daily_goal)

        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val holdDurationSeconds = prefs.getInt(SettingsActivity.KEY_HOLD_DURATION, 5)

        val dailyGoalMinutes = prefs.getInt(SettingsActivity.KEY_DAILY_GOAL, 10) // Default 10 min
        dailyGoalText.text = "Daily Goal: $dailyGoalMinutes min"

        val holdDurationMillis = holdDurationSeconds * 1000L

        titleText.text = "Hold for $holdDurationSeconds seconds to unlock $appName"
        countdownText.text = holdDurationSeconds.toString()
        progressBar.max = holdDurationSeconds * 100
        progressBar.progress = progressBar.max

        holdButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startCountdown(holdDurationMillis, countdownText, progressBar)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    cancelCountdown(holdDurationSeconds, countdownText, progressBar)
                    true
                }
                else -> false
            }
        }

        btnExit.setOnClickListener {
            unlockSuccessful = false
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(homeIntent)
            stopSelf()
        }


        return START_STICKY
    }

    private fun startCountdown(duration: Long, text: TextView, progress: ProgressBar) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duration, 10) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                text.text = secondsLeft.toString()
                progress.progress = (millisUntilFinished / 10).toInt()
            }

            override fun onFinish() {
                unlockSuccessful = true
                text.text = "0"
                progress.progress = 0
                stopSelf()
            }
        }.start()
    }

    private fun cancelCountdown(durationSeconds: Int, text: TextView, progress: ProgressBar) {
        countDownTimer?.cancel()
        text.text = durationSeconds.toString()
        progress.progress = progress.max
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        countDownTimer?.cancel()
        AppLockerService.isOverlayShowing = false

        if (unlockSuccessful) {
            AppLockerService.lastUnlockedAppPackage = currentPackageName
        }
    }
}