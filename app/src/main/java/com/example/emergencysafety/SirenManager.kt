package com.example.emergencysafety

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

class SirenManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var isSirenRunning = false

    private val flashHandler = Handler(Looper.getMainLooper())
    private var isFlashOn = false
    private val flashRunnable = object : Runnable {
        override fun run() {
            if (!isSirenRunning) return
            try {
                if (cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    isFlashOn = !isFlashOn
                    cameraManager?.setTorchMode(cameraId!!, isFlashOn)
                }
            } catch (e: Exception) {
                Log.e("SirenManager", "Flaş hatası: ${e.message}")
            }
            flashHandler.postDelayed(this, 250)
        }
    }

    fun startSiren() {
        if (isSirenRunning) return
        isSirenRunning = true

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("SirenManager", "Siren sesi hatası: ${e.message}")
        }

        try {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 500, 200, 500)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 200, 500), 0)
            }
        } catch (e: Exception) {
            Log.e("SirenManager", "Titreşim hatası: ${e.message}")
        }

        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
            flashHandler.post(flashRunnable)
        } catch (e: Exception) {
            Log.e("SirenManager", "Flaş başlatılamadı: ${e.message}")
        }
    }

    fun stopSiren() {
        if (!isSirenRunning) return
        isSirenRunning = false

        flashHandler.removeCallbacks(flashRunnable)
        try {
            if (cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager?.setTorchMode(cameraId!!, false)
            }
        } catch (e: Exception) {
            Log.e("SirenManager", "Flaş kapatılamadı: ${e.message}")
        }
        isFlashOn = false

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("SirenManager", "Siren durdurulamadı: ${e.message}")
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("SirenManager", "Titreşim durdurulamadı: ${e.message}")
        }
    }
}
