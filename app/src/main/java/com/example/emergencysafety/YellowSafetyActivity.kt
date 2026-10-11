package com.example.emergencysafety

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class YellowSafetyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createSafetyMenu()
    }

    private fun createSafetyMenu() {
        val density = resources.displayMetrics.density

        fun dp(value: Int): Int {
            return (value * density + 0.5f).toInt()
        }

        val screenBackground = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(12, 12, 12),
                Color.rgb(75, 49, 5),
                Color.rgb(175, 111, 0)
            )
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            background = screenBackground
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }

        val heading = TextView(this).apply {
            text = "GÜVENLİK MENÜSÜ"
            textSize = 25f
            setTextColor(Color.rgb(255, 218, 105))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(10), dp(4), dp(8))
        }

        content.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(this).apply {
            text = "Kendinizi güvende tutmak için bir seçenek seçin."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(4), 0, dp(4), dp(22))
        }

        content.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val options = listOf(
            "HIZLI MESAJ GÖNDER",
            "BENİ TAKİP ET / YANIMDA OL",
            "KONUMUMU PAYLAŞ",
            "GÜVENLİ YERE ULAŞMA",
            "RAHATSIZ EDİLİYORUM",
            "ACİL DURUMA GEÇ"
        )

        options.forEachIndexed { index, label ->
            val isEmergency = index == 5

            val button = TextView(this).apply {
                text = label
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER

                setTextColor(
                    if (isEmergency) {
                        Color.WHITE
                    } else {
                        Color.rgb(48, 30, 0)
                    }
                )

                setPadding(dp(12), dp(17), dp(12), dp(17))
                elevation = dp(7).toFloat()

                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    if (isEmergency) {
                        intArrayOf(
                            Color.rgb(255, 95, 75),
                            Color.rgb(190, 20, 15),
                            Color.rgb(105, 5, 5)
                        )
                    } else {
                        intArrayOf(
                            Color.rgb(255, 250, 190),
                            Color.rgb(255, 207, 55),
                            Color.rgb(190, 125, 0)
                        )
                    }
                ).apply {
                    cornerRadius = dp(17).toFloat()

                    setStroke(
                        dp(2),
                        if (isEmergency) {
                            Color.rgb(255, 175, 145)
                        } else {
                            Color.rgb(255, 232, 145)
                        }
                    )
                }

                isClickable = true
                isFocusable = true

                setOnClickListener {
                    when (index) {
                        5 -> {
                            Toast.makeText(
                                this@YellowSafetyActivity,
                                "Acil durum için ana ekrandaki kırmızı butonu kullanın.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {
                            Toast.makeText(
                                this@YellowSafetyActivity,
                                "Bu seçenek sonraki aşamada etkinleştirilecek.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(15)
            }

            content.addView(button, params)
        }

        val backButton = TextView(this).apply {
            text = "GERİ DÖN"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(20), dp(13), dp(20), dp(13))

            background = GradientDrawable().apply {
                setColor(Color.rgb(45, 38, 25))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(255, 207, 55))
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                finish()
            }
        }

        content.addView(
            backButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(scrollView)
    }
}
