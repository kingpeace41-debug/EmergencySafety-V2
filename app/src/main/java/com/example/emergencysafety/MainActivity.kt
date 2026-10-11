package com.example.emergencysafety

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val volumeDownHandler = Handler(Looper.getMainLooper())
    private var volumeDownRunnable: Runnable? = null

    private val instructionPrefs by lazy {
        getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissionsAndStartService()
        checkOverlayPermission()

        val btnRedAlertId =
            resources.getIdentifier("btnRedAlert", "id", packageName)

        if (btnRedAlertId != 0) {
            findViewById<View>(btnRedAlertId)?.setOnClickListener {
                vibrateOnClick()
                openRedMenuWithInstruction()
            }
        }

        val btnToggleListeningId =
            resources.getIdentifier("btnToggleListening", "id", packageName)

        if (btnToggleListeningId != 0) {
            findViewById<View>(btnToggleListeningId)?.setOnClickListener {
                vibrateOnClick()
                toggleVoiceService()
            }
        }
    }

    private fun openRedMenuWithInstruction() {
        val dontShowAgain = instructionPrefs.getBoolean(
            "dont_show_red_instructions",
            false
        )

        if (dontShowAgain) {
            openRedSettings()
        } else {
            showRedInstructions()
        }
    }

    private fun openRedSettings() {
        startActivity(
            Intent(this, SettingsCategoriesActivity::class.java)
        )
    }

    private fun showRedInstructions() {
        val density = resources.displayMetrics.density

        fun dp(value: Int): Int =
            (value * density + 0.5f).toInt()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(12), dp(22), dp(4))
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = false
            addView(content)
        }

        val heading = TextView(this).apply {
            text = "KIRMIZI BUTON\nKULLANIM TALİMATI"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(190, 32, 43))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
        }
        content.addView(heading)

        val instructionText = """
            ACİL DURUM AYARLARI

            Kırmızı buton, uygulamanın acil durum ayarlarına ulaşmanızı sağlar. İlk kullanımdan önce seçenekleri inceleyin.

            1. ACİL DURUM KİŞİLERİ

            Kişi ve mesaj ayarları bölümünden sunulan alanları doldurun. Telefon numaralarını doğru yazdığınızdan emin olun. Yardım isteyebileceğiniz kişileri önceden belirleyin.

            2. ACİL DURUM MESAJI

            Mesaj ayarlarında hazır mesajı inceleyin ve düzenleme seçeneği varsa ihtiyacınıza göre değiştirin. Değişikliklerinizi kaydedin. Mesajın gönderilmesi, uygulamanın sunduğu gönderim yöntemine ve gerekli koşullara bağlıdır.

            3. ACİL DURUM KODU

            Sesli komut ayarlarında tanımlı kodları inceleyin. Kod seçme veya değiştirme seçeneği varsa günlük konuşmalarda az kullanılan bir kelime ve iki haneli sayı tercih edebilirsiniz.

            Örnek: “Leylek 41”.

            Bu yalnızca örnektir. Uygulamanın tanıdığı kodu kullanın ve seçtiğiniz kodu güvenli bir şekilde hatırlayın.

            4. SİREN, FLAŞ VE TİTREŞİM

            Acil durum alarmı etkinleştiğinde uygulamanın siren sesi çalar, flaş yanıp söner ve komutun alındığını bildiren kısa bir titreşim oluşur. Siren çevrenizdekilerin dikkatini çekmeye ve arama-kurtarma çalışmalarında yerinizin fark edilmesine yardımcı olmayı amaçlar.

            5. SESLİ KOMUTLAR

            Sesli komut özelliği için gerekli mikrofon iznini verin. Uygulamada tanımlı komutları önceden öğrenin ve güvenli bir ortamda deneyin. Sürekli dinleme özelliğini ana ekrandaki ilgili kontrolle açıp kapatabilirsiniz.

            6. ALARMI DURDURMA

            Alarmı durdurmak için uygulamada tanımlı durdurma komutunu veya ilgili kontrolü kullanın. Bu sürümde ses kısma tuşunu yaklaşık iki saniye basılı tutmak da sireni durdurmak için kullanılabilir.

            ÖNEMLİ GÜVENLİK UYARISI

            Siren tek başına yetkililere konum veya yardım çağrısı göndermez. Konum paylaşımı, otomatik fotoğraf çekimi ve ses kaydı bu talimatta etkin özellikler olarak belirtilmemektedir.

            Uygulama resmî acil yardım hizmetlerinin yerine geçmez ve yardımın ulaşacağını garanti etmez. Hayati tehlike durumunda Türkiye'de 112 Acil Çağrı Merkezi'ni arayın.
        """.trimIndent()

        val body = TextView(this).apply {
            text = instructionText
            textSize = 15f
            setTextColor(Color.rgb(40, 40, 40))
            setLineSpacing(dp(3).toFloat(), 1.0f)
            setPadding(0, 0, 0, dp(12))
        }
        content.addView(body)

        val dontShowCheckbox = CheckBox(this).apply {
            text = "Bir daha gösterme"
            textSize = 15f
            setTextColor(Color.rgb(35, 35, 35))
            setPadding(0, dp(8), 0, dp(8))
            isChecked = false
        }
        content.addView(dontShowCheckbox)

        val dialog = AlertDialog.Builder(this)
            .setView(scrollView)
            .setPositiveButton("KAPAT", null)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.WHITE)
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                background = GradientDrawable().apply {
                    setColor(Color.rgb(190, 32, 43))
                    cornerRadius = dp(10).toFloat()
                }
                setPadding(dp(20), dp(10), dp(20), dp(10))

                setOnClickListener {
                    if (dontShowCheckbox.isChecked) {
                        instructionPrefs.edit()
                            .putBoolean(
                                "dont_show_red_instructions",
                                true
                            )
                            .apply()
                    }

                    dialog.dismiss()
                    openRedSettings()
                }
            }
        }

        dialog.show()
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
