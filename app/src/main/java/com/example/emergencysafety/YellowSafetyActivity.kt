package com.example.emergencysafety

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class YellowSafetyActivity : AppCompatActivity() {

    private val prefsName = "EmergencyContactSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yellow_safety)

        findViewById<ImageView>(R.id.btnYellowHelp)
            .setOnClickListener {
                sendHelpToFirstContact()
            }

        findViewById<ImageView>(R.id.btnYellowLocation)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Konum paylaşımını sonraki aşamada bağlayacağız.",
                    Toast.LENGTH_LONG
                ).show()
            }

        findViewById<ImageView>(R.id.btnYellowSafetySupport)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Güvenlik desteğini sonraki aşamada bağlayacağız.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun sendHelpToFirstContact() {
        val prefs = getSharedPreferences(
            prefsName,
            MODE_PRIVATE
        )

        val name = prefs.getString("contact1_name", "")?.trim().orEmpty()
        val phone = prefs.getString("contact1_phone", "")?.trim().orEmpty()

        if (phone.isBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Acil kişi kayıtlı değil")
                .setMessage(
                    "Lütfen kırmızı menüdeki Rehber ve Mesaj Ayarları " +
                    "bölümünden birinci kişinin adını ve telefon numarasını kaydet."
                )
                .setPositiveButton("TAMAM", null)
                .show()
            return
        }

        val message = prefs.getString(
            "emergency_message",
            ""
        )?.trim().orEmpty().ifBlank {
            "Acil yardıma ihtiyacım var. Lütfen benimle iletişime geç."
        }

        val normalizedPhone = normalizePhoneNumber(phone)

        if (normalizedPhone.isBlank()) {
            Toast.makeText(
                this,
                "Birinci kişinin telefon numarası geçerli görünmüyor.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val encodedMessage = Uri.encode(message)
        val whatsappUri = Uri.parse(
            "https://wa.me/$normalizedPhone?text=$encodedMessage"
        )

        val intent = Intent(Intent.ACTION_VIEW, whatsappUri)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "WhatsApp açılamadı. WhatsApp'ın kurulu olduğunu kontrol et.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "WhatsApp açılamadı. Lütfen tekrar dene.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun normalizePhoneNumber(rawPhone: String): String {
        var phone = rawPhone.filter { it.isDigit() || it == '+' }

        if (phone.startsWith("00")) {
            phone = phone.removePrefix("00")
        }

        if (phone.startsWith("+")) {
            phone = phone.drop(1).filter { it.isDigit() }
        } else {
            phone = phone.filter { it.isDigit() }

            when {
                phone.length == 11 && phone.startsWith("0") -> {
                    phone = "90" + phone.drop(1)
                }

                phone.length == 10 && phone.startsWith("5") -> {
                    phone = "90$phone"
                }
            }
        }

        return phone
    }
}
