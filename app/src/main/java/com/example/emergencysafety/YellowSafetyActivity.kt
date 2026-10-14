package com.example.emergencysafety

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class YellowSafetyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yellow_safety)

        findViewById<ImageView>(R.id.btnYellowHelp)
            .setOnClickListener {
                showNotReadyMessage(
                    "Yardım isteme işlevini sonraki aşamada bağlayacağız."
                )
            }

        findViewById<ImageView>(R.id.btnYellowLocation)
            .setOnClickListener {
                showNotReadyMessage(
                    "Konum paylaşımını sonraki aşamada bağlayacağız."
                )
            }

        findViewById<ImageView>(R.id.btnYellowSafetySupport)
            .setOnClickListener {
                showNotReadyMessage(
                    "Güvenlik desteğini sonraki aşamada bağlayacağız."
                )
            }
    }

    private fun showNotReadyMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
