package com.example.emergencysafety

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        return text.lowercase(Locale("tr", "TR"))
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun getCommand(key: String, defaultValue: String): String {
        val prefs = context.getSharedPreferences(
            prefsName,
            Context.MODE_PRIVATE
        )
        return normalize(prefs.getString(key, defaultValue) ?: defaultValue)
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
            Log.e("VoiceTriggerManager", "Ses tanıma desteklenmiyor.")
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                context.packageName
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
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

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
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

                        when {
                            spoken == redStart -> {
                                onRedStartCallback?.invoke()
                                break
                            }

                            spoken == redStop -> {
                                onRedStopCallback?.invoke()
                                break
                            }

                            spoken == sirenStart -> {
                                onSirenStartCallback?.invoke()
                                break
                            }

                            spoken == sirenStop -> {
                                onSirenStopCallback?.invoke()
                                break
                            }
                        }
                    }
                }

                startListeningSafely()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
