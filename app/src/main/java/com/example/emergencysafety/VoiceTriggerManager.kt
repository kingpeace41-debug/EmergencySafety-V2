package com.example.emergencysafety

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
    private var isStartingListening = false

    private val prefsName = "VoiceCommandSettings"

    private var pendingCommand: String? = null
    private var pendingCommandTime = 0L

    private val confirmationWindowMs = 3000L

    private fun normalize(text: String): String {
        var result = text.lowercase(Locale("tr", "TR"))
            .replace('ı', 'i')
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        result = result
            .replace(
                Regex("(?<!\\p{L})kirk\\s*bir(?!\\p{L})"),
                "41"
            )
            .replace(
                Regex("(?<!\\p{L})kirk\\s*iki(?!\\p{L})"),
                "42"
            )

        return result.replace(Regex("\\s+"), " ").trim()
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

    private fun vibrate(pattern: LongArray) {
        try {
            val vibrator = context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as? Vibrator

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (exception: Exception) {
            Log.e(
                "VoiceTriggerManager",
                "Titreşim verilemedi.",
                exception
            )
        }
    }

    private fun matchesCommand(
        spoken: String,
        configuredCommand: String
    ): Boolean {
        if (spoken.isBlank() || configuredCommand.isBlank()) {
            return false
        }

        // Önce tam eşleşmeyi dene.
        if (spoken == configuredCommand) {
            return true
        }

        // Birden fazla kelimeden oluşan komutlarda,
        // komutun kelimeleri algılanan cümlede arka arkaya bulunmalı.
        val commandWords = configuredCommand.split(" ")
        val spokenWords = spoken.split(" ")

        if (commandWords.isEmpty() || spokenWords.size < commandWords.size) {
            return false
        }

        return spokenWords
            .windowed(commandWords.size)
            .any { it == commandWords }
    }

    private fun findCommand(spokenText: String): Pair<String, (() -> Unit)>? {
        val spoken = normalize(spokenText)

        val commands = listOf(
            Triple(
                "red_start",
                getCommand("red_start", "kırmızı 41"),
                onRedStartCallback
            ),
            Triple(
                "red_stop",
                getCommand("red_stop", "kırmızı 42"),
                onRedStopCallback
            ),
            Triple(
                "siren_start",
                getCommand("siren_start", "siren aç"),
                onSirenStartCallback
            ),
            Triple(
                "siren_stop",
                getCommand("siren_stop", "siren kapat"),
                onSirenStopCallback
            )
        )

        for ((key, configured, callback) in commands) {
            if (callback != null && matchesCommand(spoken, configured)) {
                return Pair(key, callback)
            }
        }

        return null
    }

    private fun handleCommand(
        commandKey: String,
        action: () -> Unit
    ) {
        val now = SystemClock.elapsedRealtime()

        if (
            pendingCommand == commandKey &&
            now - pendingCommandTime <= confirmationWindowMs
        ) {
            pendingCommand = null
            pendingCommandTime = 0L

            Log.d(
                "VoiceTriggerManager",
                "Komut iki kez doğrulandı: $commandKey"
            )

            vibrate(longArrayOf(0, 60, 80, 60))
            action()
        } else {
            pendingCommand = commandKey
            pendingCommandTime = now

            Log.d(
                "VoiceTriggerManager",
                "İlk algılama; ikinci söyleyiş bekleniyor: $commandKey"
            )

            vibrate(longArrayOf(0, 45))
        }
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
                5
            )
        }

        startListeningSafely()
    }

    private fun startListeningSafely() {
        if (!isListening || isStartingListening) return

        val recognizer = speechRecognizer ?: return
        val intent = recognizerIntent ?: return

        try {
            isStartingListening = true
            recognizer.startListening(intent)
        } catch (exception: Exception) {
            Log.e(
                "VoiceTriggerManager",
                "Dinleme başlatılamadı.",
                exception
            )
        } finally {
            isStartingListening = false
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
                Log.e(
                    "VoiceTriggerManager",
                    "Ses tanıma hatası: $error"
                )

                if (isListening) {
                    startListeningSafely()
                }
            }

            override fun onResults(results: Bundle?) {
                try {
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    if (matches != null && isListening) {
                        var found: Pair<String, (() -> Unit)>? = null

                        for (recognizedText in matches) {
                            Log.d(
                                "VoiceTriggerManager",
                                "Algılanan metin: $recognizedText"
                            )

                            found = findCommand(recognizedText)

                            if (found != null) break
                        }

                        if (found != null) {
                            handleCommand(found.first, found.second)
                        }
                    }
                } finally {
                    if (isListening) {
                        startListeningSafely()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(
                eventType: Int,
                params: Bundle?
            ) {}
        }

    fun stopListening() {
        isListening = false
        isStartingListening = false
        pendingCommand = null
        pendingCommandTime = 0L

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (exception: Exception) {
            Log.e(
                "VoiceTriggerManager",
                "Ses tanıma kapatılırken hata oluştu.",
                exception
            )
        }

        speechRecognizer = null
        recognizerIntent = null
    }
}
