package com.example.emergencysafety

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val volumeDownHandler = Handler(Looper.getMainLooper())
    private var volumeDownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissionsAndStartService()
        checkOverlayPermission()

        val btnRedAlertId =
            resources.getIdentifier("btnRedAlert", "id", packageName)

        if (btnRedAlertId != 0) {
            val btnRedAlert = findViewById<View>(btnRedAlertId)

            btnRedAlert?.setOnClickListener {
                vibrateOnClick()
                startActivity(
                    Intent(this, SettingsCategoriesActivity::class.java)
                )
            }
        }

        val btnToggleListeningId =
            resources.getIdentifier("btnToggleListening", "id", packageName)

        if (btnToggleListeningId != 0) {
            val btnToggleListening = findViewById<View>(btnToggleListeningId)

            btnToggleListening?.setOnClickListener {
                vibrateOnClick()
                toggleVoiceService()
            }
        }
    }

    private fun toggleVoiceService() {
        val sharedPref =
            getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val isCurrentlyActive =
            sharedPref.getBoolean("is_voice_active", true)

        if (isCurrentlyActive) {
            stopVoiceService()
            sharedPref.edit()
                .putBoolean("is_voice_active", false)
                .apply()

            Toast.makeText(
                this,
                "Sürekli Dinleme KAPATILDI (Pasif Mod)",
                Toast.LENGTH_LONG
            ).show()
        } else {
            sharedPref.edit()
                .putBoolean("is_voice_active", true)
                .apply()

            startVoiceService()

            Toast.makeText(
                this,
                "Sürekli Dinleme BAŞLATILDI (Aktif Mod)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun vibrateOnClick() {
        val vibrator =
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun checkPermissionsAndStartService() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                101
            )
        } else {
            val sharedPref =
                getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

            val isVoiceActive =
                sharedPref.getBoolean("is_voice_active", true)

            if (isVoiceActive) {
                startVoiceService()
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 102)
            }
        }
    }

    private fun startVoiceService() {
        val serviceIntent =
            Intent(this, VoiceTriggerService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopVoiceService() {
        val serviceIntent =
            Intent(this, VoiceTriggerService::class.java)

        stopService(serviceIntent)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event?.repeatCount == 0) {
                volumeDownRunnable = Runnable {
                    stopSirenService()
                    Toast.makeText(
                        this,
                        "Siren durduruldu",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                volumeDownHandler.postDelayed(
                    volumeDownRunnable!!,
                    2000
                )
            }

            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDownRunnable?.let {
                volumeDownHandler.removeCallbacks(it)
            }

            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    private fun stopSirenService() {
        val serviceIntent =
            Intent(this, VoiceTriggerService::class.java).apply {
                action = "ACTION_STOP_SIREN"
            }

        startService(serviceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 101) {
            val sharedPref =
                getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

            val isVoiceActive =
                sharedPref.getBoolean("is_voice_active", true)

            if (isVoiceActive) {
                startVoiceService()
            }
        }
    }
}
