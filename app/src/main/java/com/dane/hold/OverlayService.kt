package com.dane.hold

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import com.dane.hold.data.LockedAppManager

class OverlayService: Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var currentPackageName: String? = null
    private var unlockSuccessful = false
    private lateinit var vibrator: Vibrator
    private var countDownTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra("APP_NAME") ?: "this app"
        currentPackageName = intent?.getStringExtra("PACKAGE_NAME")

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, layoutFlag, 0, PixelFormat.TRANSLUCENT)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        setupUI()

        val titleText: TextView = overlayView.findViewById(R.id.text_overlay_title)
        val holdButton: Button = overlayView.findViewById(R.id.button_hold)
        val countdownText: TextView = overlayView.findViewById(R.id.text_countdown)
        val btnExit: Button = overlayView.findViewById(R.id.btn_exit)
        val dailyGoalText: TextView = overlayView.findViewById(R.id.text_daily_goal)

        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        var holdDurationSeconds = LockedAppManager.getLockedAppDuration(this, currentPackageName ?: "")

        // Jika tidak ada durasi kustom (-1), ambil durasi default dari pengaturan.
        if (holdDurationSeconds == -1) {
            holdDurationSeconds = prefs.getInt(SettingsActivity.KEY_HOLD_DURATION, 5)
        }

        val holdDurationMillis = holdDurationSeconds * 1000L
        val dailyGoalMinutes = prefs.getInt(SettingsActivity.KEY_DAILY_GOAL, 10)

        titleText.text = "Hold for $holdDurationSeconds seconds to unlock $appName"
        dailyGoalText.text = "Daily Goal: $dailyGoalMinutes min"
        countdownText.text = holdDurationSeconds.toString()

        holdButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startScalingAnimation(holdButton, countdownText, holdDurationMillis)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    cancelScalingAnimation(holdButton, countdownText)
                    countdownText.text = holdDurationSeconds.toString()
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

    private fun startScalingAnimation(button: Button, countdownText: TextView, duration: Long) {
        vibrate(50)
        button.text = ""
        countdownText.visibility = View.VISIBLE


        countDownTimer = object : CountDownTimer(duration, 16) { // Update ~60fps
            override fun onTick(millisUntilFinished: Long) {
                // 1. Update Teks Countdown
                val secondsLeft = Math.ceil(millisUntilFinished / 1000.0).toInt() - 1
                countdownText.text = secondsLeft.toString()

                // 2. Hitung Progres (0.0 -> 1.0)
                val progress = (duration - millisUntilFinished).toFloat() / duration.toFloat()

                // 3. Hitung skala target (dari 1x menjadi 2x ukuran)
                val currentScale = 1.0f + progress // 1.0f adalah ukuran asli, 1.0f + 1.0f = 2.0f

                // 4. Terapkan skala ke tombol secara manual
                button.scaleX = currentScale
                button.scaleY = currentScale
            }

            override fun onFinish() {
                countdownText.text = "0"
                vibrate(150)
                unlockSuccessful = true

                // Add a short delay so the user can see the "0" before the overlay disappears
                Handler(Looper.getMainLooper()).postDelayed({
                    stopSelf()
                }, 200)
            }
        }.start()
    }

    private fun cancelScalingAnimation(button: Button, countdownText: TextView) {
        // Hentikan animasi dan timer
        button.animate().cancel()
        countDownTimer?.cancel()

        // Kembalikan tombol ke keadaan semula
        button.scaleX = 1.0f
        button.scaleY = 1.0f
        button.text = "HOLD"
    }

    @Suppress("DEPRECATION")
    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun setupUI() {
        overlayView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        overlayView.isFocusableInTouchMode = true
        overlayView.requestFocus()
        overlayView.setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
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