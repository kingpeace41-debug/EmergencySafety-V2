package com.example.emergencysafety

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceTriggerManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private var onRedStartCallback: (() -> Unit)? = null
    private var onRedStopCallback: (() -> Unit)? = null
    private var onSirenStartCallback: (() -> Unit)? = null
    private var onSirenStopCallback: (() -> Unit)? = null

    private var isListening = false

    private val prefsName = "VoiceCommandSettings"

    private fun normalize(text: String): String {
        var result = text.lowercase(Locale("tr", "TR"))
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        result = result
            .replace(
                Regex("(?<!\\p{L})kırk\\s*bir(?!\\p{L})"),
                "41"
            )
            .replace(
                Regex("(?<!\\p{L})kırk\\s*iki(?!\\p{L})"),
                "42"
            )

        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun giveRecognitionFeedback() {
        try {
            val vibrator = context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as? Vibrator

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(45, 100)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(45)
                }
            }
        } catch (exception: Exception) {
            Log.e(
                "VoiceTriggerManager",
                "Geri bildirim titreşimi verilemedi.",
                exception
            )
        }
    }

    private fun getCommand(
        key: String,
        defaultValue: String
    ): String {
        val prefs = context.getSharedPreferences(
            prefsName,
            Context.MODE_PRIVATE
        )
        return normalize(
            prefs.getString(key, defaultValue) ?: defaultValue
        )
    }

    fun startListening(
        onRedStart: () -> Unit,
        onRedStop: () -> Unit,
        onSirenStart: () -> Unit,
        onSirenStop: () -> Unit
    ) {
        onRedStartCallback = onRedStart
        onRedStopCallback = onRedStop
        onSirenStartCallback = onSirenStart
        onSirenStopCallback = onSirenStop
        isListening = true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(
                "VoiceTriggerManager",
                "Ses tanıma desteklenmiyor."
            )
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
        }

        recognizerIntent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "tr-TR"
            )
            putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                context.packageName
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            )
        }

        startListeningSafely()
    }

    private fun startListeningSafely() {
        if (isListening) {
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (exception: Exception) {
                Log.e(
                    "VoiceTriggerManager",
                    "Dinleme başlatılamadı.",
                    exception
                )
            }
        }
    }

    private fun createRecognitionListener() =
        object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VoiceTriggerManager", "Dinlemeye hazır.")
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.e(
                    "VoiceTriggerManager",
                    "Ses tanıma hatası: $error"
                )
                startListeningSafely()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                if (matches != null && isListening) {
                    val redStart = getCommand(
                        "red_start",
                        "kırmızı 41"
                    )
                    val redStop = getCommand(
                        "red_stop",
                        "kırmızı 42"
                    )
                    val sirenStart = getCommand(
                        "siren_start",
                        "siren aç"
                    )
                    val sirenStop = getCommand(
                        "siren_stop",
                        "siren kapat"
                    )

                    for (recognizedText in matches) {
                        val spoken = normalize(recognizedText)

                        Log.d(
                            "VoiceTriggerManager",
                            "Algılanan: '$recognizedText' -> '$spoken'"
                        )

                        when (spoken) {
                            redStart -> {
                                giveRecognitionFeedback()
                                onRedStartCallback?.invoke()
                                break
                            }

                            redStop -> {
                                giveRecognitionFeedback()
                                onRedStopCallback?.invoke()
                                break
                            }

                            sirenStart -> {
                                giveRecognitionFeedback()
                                onSirenStartCallback?.invoke()
                                break
                            }

                            sirenStop -> {
                                giveRecognitionFeedback()
                                onSirenStopCallback?.invoke()
                                break
                            }
                        }
                    }
                }

                startListeningSafely()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(
                eventType: Int,
                params: Bundle?
            ) {}
        }

    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
