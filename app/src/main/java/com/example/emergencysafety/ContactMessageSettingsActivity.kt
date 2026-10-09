package com.example.emergencysafety

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ContactMessageSettingsActivity : AppCompatActivity() {

    private val prefsName = "EmergencyContactSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_message_settings)

        val fields = listOf(
            R.id.etContact1Name,
            R.id.etContact1Phone,
            R.id.etContact2Name,
            R.id.etContact2Phone,
            R.id.etContact3Name,
            R.id.etContact3Phone,
            R.id.etEmergencyMessage
        )

        val keys = listOf(
            "contact1_name",
            "contact1_phone",
            "contact2_name",
            "contact2_phone",
            "contact3_name",
            "contact3_phone",
            "emergency_message"
        )

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val inputs = fields.map { findViewById<EditText>(it) }

        inputs.forEachIndexed { index, input ->
            input.setText(prefs.getString(keys[index], ""))
        }

        findViewById<Button>(R.id.btnSaveContacts).setOnClickListener {
            val editor = prefs.edit()

            inputs.forEachIndexed { index, input ->
                editor.putString(keys[index], input.text.toString().trim())
            }

            editor.apply()
            Toast.makeText(this, "Rehber ve mesaj ayarları kaydedildi", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnBackContacts).setOnClickListener {
            finish()
        }
    }
}
