
package com.example.emergencysafety

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class VoiceTriggerService : Service() {

    private lateinit var sirenManager: SirenManager
    private lateinit var voiceTriggerManager: VoiceTriggerManager

    override fun onCreate() {
        super.onCreate()

        sirenManager = SirenManager(this)
        voiceTriggerManager = VoiceTriggerManager(this)

        startForegroundServiceWithNotification()

        voiceTriggerManager.startListening(
            onRedStart = { triggerRedCode() },
            onRedStop = { stopRedCode() },
            onSirenStart = { triggerSirenCode() },
            onSirenStop = { stopSirenCode() }
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            "ACTION_STOP_SIREN" -> stopSirenCode()
            "ACTION_STOP_RED" -> stopRedCode()
        }

        return START_STICKY
    }

    private fun triggerRedCode() {
        sendRedModeState(true)

        val intent = Intent(
            this,
            FakeDeadActivity::class.java
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(intent)
    }

    private fun stopRedCode() {
        // Kırmızı modu pasif olarak ana ekrana bildir.
        sendRedModeState(false)

        // Siyah ekranı kapat.
        val stopBroadcast = Intent(
            "com.example.emergencysafety.CLOSE_FAKE_DEAD"
        ).apply {
            setPackage(packageName)
        }

        sendBroadcast(stopBroadcast)

        val closeIntent = Intent(
            this,
            FakeDeadActivity::class.java
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("EXTRA_CLOSE", true)
        }

        startActivity(closeIntent)
    }

    private fun sendRedModeState(active: Boolean) {
        val stateIntent = Intent(
            "com.example.emergencysafety.RED_MODE_CHANGED"
        ).apply {
            setPackage(packageName)
            putExtra("RED_MODE_ACTIVE", active)
        }

        sendBroadcast(stateIntent)
    }

    private fun triggerSirenCode() {
        sirenManager.startSiren()
    }

    private fun stopSirenCode() {
        sirenManager.stopSiren()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "voice_trigger_channel"

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ses Algılama Servisi",
                NotificationManager.IMPORTANCE_LOW
            )

            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("Emergency Safety Aktif")
                .setContentText("Ses komutları dinleniyor...")
                .setSmallIcon(R.drawable.ic_app_logo)
                .setOngoing(true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        voiceTriggerManager.stopListening()
        sirenManager.stopSiren()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
