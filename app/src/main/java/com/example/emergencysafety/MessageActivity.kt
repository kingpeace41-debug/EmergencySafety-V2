
package com.example.emergencysafety

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MessageActivity : AppCompatActivity() {

    private lateinit var messageList: LinearLayout
    private lateinit var messageInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        messageList = findViewById(R.id.messageList)
        messageInput = findViewById(R.id.messageInput)

        findViewById<ImageButton>(R.id.messageBack).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.messageSend).setOnClickListener {
            val text = messageInput.text.toString().trim()

            if (text.isNotEmpty()) {
                addMessageBubble(text)
                messageInput.text.clear()

                Toast.makeText(
                    this,
                    "Bu ilk taslakta mesaj yalnızca bu telefonda gösteriliyor. Gerçek mesaj gönderimi sonraki aşamada eklenecek.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addMessageBubble(text: String) {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(10), dp(14), dp(10))

            background = GradientDrawable().apply {
                setColor(Color.rgb(23, 105, 74))
                cornerRadius = dp(18).toFloat()
            }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(dp(8), dp(4), dp(8), dp(4))

            addView(
                bubble,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        messageList.addView(row)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
