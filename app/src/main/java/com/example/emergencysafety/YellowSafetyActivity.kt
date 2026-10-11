package com.example.emergencysafety

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class YellowSafetyActivity : AppCompatActivity() {

    private val gold = Color.rgb(255, 193, 7)
    private val darkGold = Color.rgb(150, 95, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showMenu()
    }

    private fun showMenu() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density + 0.5f).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(12, 12, 12),
                    Color.rgb(65, 43, 5),
                    Color.rgb(190, 125, 0)
                )
            )
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        val title = TextView(this).apply {
            text = "GÜVENLİK MENÜSÜ"
            textSize = 25f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(8))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Kendinizi güvende tutmak için bir seçenek seçin"
            textSize = 14f
            setTextColor(Color.rgb(255, 230, 160))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(22))
        }
        root.addView(subtitle)

        val options = listOf(
            "HIZLI MESAJ GÖNDER",
            "BENİ TAKİP ET / YANIMDA OL",
            "KONUMUMU PAYLAŞ",
            "GÜVENLİ YERE ULAŞMA",
            "RAHATSIZ EDİLİYORUM",
            "ACİL DURUMA GEÇ"
        )

        options.forEachIndexed { index, label ->
            val button = TextView(this).apply {
                text = label
                textSize = 16f
                setTextColor(
                    if (index == 5) Color.WHITE else Color.BLACK
                )
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(18), dp(12), dp(18))
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    if (index == 5) {
                        intArrayOf(
                            Color.rgb(230, 55, 35),
                            Color.rgb(155, 15, 15)
                        )
                    } else {
                        intArrayOf(
                            Color.rgb(255, 240, 170),
                            gold,
                            darkGold
                        )
                    }
                ).apply {
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(2), Color.rgb(255, 222, 120))
                }
                elevation = dp(7).toFloat()
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (index == 5) {
                        finish()
                    } else {
                        android.widget.Toast.makeText(
                            this@YellowSafetyActivity,
                            "Bu seçenek sonraki aşamada etkinleştirilecek.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(13)
            }
            root.addView(button, params)
        }

        val back = TextView(this).apply {
            text = "GERİ DÖN"
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(14), dp(20), dp(14))
            setOnClickListener { finish() }
        }
        root.addView(back)

        setContentView(scroll)
    }
}
