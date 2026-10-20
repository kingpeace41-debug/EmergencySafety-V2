package com.example.emergencysafety

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class VoiceTriggerService : Service() {

    companion object {
        private const val TAG = "EmergencySafetyVoice"
        private const val CHANNEL_ID = "voice_trigger_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var sirenManager: SirenManager
    private lateinit var voiceTriggerManager: VoiceTriggerManager

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        Log.i(TAG, "Kırmızı kod doğrulandı; acil mesaj gönderimi başlatılıyor.")

        sendEmergencyMessages()

        val intent = Intent(this, FakeDeadActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(intent)
    }

    private fun sendEmergencyMessages() {
        val contactPrefs = getSharedPreferences(
            "EmergencyContactSettings",
            Context.MODE_PRIVATE
        )

        val message = contactPrefs
            .getString("emergency_message", "")
            ?.trim()
            .orEmpty()

        val contacts = (1..3).mapNotNull { index ->
            val name = contactPrefs
                .getString("contact${index}_name", "")
                ?.trim()
                .orEmpty()

            val phone = contactPrefs
                .getString("contact${index}_phone", "")
                ?.trim()
                .orEmpty()

            if (phone.isNotBlank()) {
                Pair(name.ifBlank { "Kişi $index" }, phone)
            } else {
                null
            }
        }

        val connectionPrefs = getSharedPreferences(
            "EmergencySafetyConnection",
            Context.MODE_PRIVATE
        )

        val apiKey = connectionPrefs
            .getString("worker_api_key", "")
            ?.trim()
            .orEmpty()

        if (message.isBlank()) {
            Log.e(TAG, "Gönderim durdu: Acil durum mesajı kaydedilmemiş.")
            updateNotification("Acil durum mesajı ayarlanmamış.")
            return
        }

        if (contacts.isEmpty()) {
            Log.e(TAG, "Gönderim durdu: Acil durum kişisi bulunamadı.")
            updateNotification("Acil durum kişileri ayarlanmamış.")
            return
        }

        if (apiKey.isBlank()) {
            Log.e(TAG, "Gönderim durdu: Worker anahtarı bulunamadı.")
            updateNotification("Worker bağlantı ayarı eksik.")
            return
        }

        serviceScope.launch {
            var succeeded = 0
            val errors = mutableListOf<String>()

            updateNotification("Acil durum mesajları gönderiliyor...")

            for ((name, originalPhone) in contacts) {
                val phone = normalizePhoneNumber(originalPhone)

                if (phone.isBlank()) {
                    errors.add("$name: telefon numarası geçersiz")
                    continue
                }

                val result = WhatsAppWorkerClient.sendMessage(
                    phoneNumber = phone,
                    message = message,
                    apiKey = apiKey
                )

                result.fold(
                    onSuccess = { response ->
                        val accepted = try {
                            JSONObject(response)
                                .optBoolean("success", false)
                        } catch (_: Exception) {
                            false
                        }

                        if (accepted) {
                            succeeded++
                            Log.i(
                                TAG,
                                "$name: Meta gönderim isteğini kabul etti."
                            )
                        } else {
                            errors.add("$name: beklenmeyen Worker yanıtı")
                            Log.e(
                                TAG,
                                "$name: Beklenmeyen yanıt: $response"
                            )
                        }
                    },
                    onFailure = { error ->
                        val detail = error.message
                            ?: "Bilinmeyen gönderim hatası"

                        errors.add("$name: $detail")
                        Log.e(
                            TAG,
                            "$name: WhatsApp gönderimi başarısız.",
                            error
                        )
                    }
                )
            }

            val summary =
                "Gönderim isteği: $succeeded/${contacts.size}"

            Log.i(TAG, summary)

            if (errors.isNotEmpty()) {
                Log.e(TAG, errors.joinToString(" | "))
            }

            updateNotification(summary)
        }
    }

    private fun normalizePhoneNumber(rawPhone: String): String {
        var digits = rawPhone.filter { it.isDigit() }

        if (digits.startsWith("00")) {
            digits = digits.drop(2)
        }

        return when {
            digits.startsWith("90") && digits.length == 12 -> digits

            digits.startsWith("0") && digits.length == 11 ->
                "90${digits.drop(1)}"

            digits.startsWith("5") && digits.length == 10 ->
                "90$digits"

            else -> digits
        }
    }

    private fun stopRedCode() {
        Log.i(TAG, "Kırmızı kod durduruluyor; sesli dinleme kapatılacak.")

        // Ana ekrandaki sesli dinleme ayarını kapat.
        getSharedPreferences(
            "AppSettings",
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean("is_voice_active", false)
            .apply()

        // Karartılmış ekranı kapatma sinyali.
        val stopBroadcast =
            Intent("com.example.emergencysafety.CLOSE_FAKE_DEAD").apply {
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

        // Ekran kapatma başarısız olsa bile servisi durdur.
        try {
            startActivity(closeIntent)
        } catch (error: Exception) {
            Log.e(TAG, "Kırmızı ekran kapatılamadı.", error)
        } finally {
            stopSelf()
        }
    }

    private fun triggerSirenCode() {
        sirenManager.startSiren()
    }

    private fun stopSirenCode() {
        sirenManager.stopSiren()
    }

    private fun startForegroundServiceWithNotification() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ses Algılama Servisi",
                NotificationManager.IMPORTANCE_LOW
            )

            notificationManager.createNotificationChannel(channel)
        }

        val notification = buildNotification(
            "Ses komutları dinleniyor..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(text)
        )
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency Safety")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        voiceTriggerManager.stopListening()
        sirenManager.stopSiren()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
