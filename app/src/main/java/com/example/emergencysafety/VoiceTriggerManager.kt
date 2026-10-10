package com.example.emergencysafety

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val prefsName = "VoiceCommandSettings"

    private var pendingCommand: String? = null
    private var pendingCommandTime = 0L
    private val confirmationWindowMs = 3000L

    private val handler = Handler(Looper.getMainLooper())
    private var restartPending = false

    private fun normalize(text: String): String {
        var result = text.lowercase(Locale("tr", "TR"))
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        result = result
            .replace(Regex("(?<!\\p{L})kırk\\s*bir(?!\\p{L})"), "41")
            .replace(Regex("(?<!\\p{L})kırk\\s*iki(?!\\p{L})"), "42")

        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun getCommand(key: String, defaultValue: String): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return normalize(prefs.getString(key, defaultValue) ?: defaultValue)
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val vibrator =
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (exception: Exception) {
            Log.e("VoiceTriggerManager", "Titreşim verilemedi.", exception)
        }
    }

    private data class VoiceCommand(
        val key: String,
        val phrase: String,
        val action: () -> Unit
    )

    private fun getCommands(): List<VoiceCommand> {
        val commands = mutableListOf<VoiceCommand>()

        onRedStartCallback?.let {
            commands.add(
                VoiceCommand("red_start", getCommand("red_start", "kırmızı 41"), it)
            )
        }
        onRedStopCallback?.let {
            commands.add(
                VoiceCommand("red_stop", getCommand("red_stop", "kırmızı 42"), it)
            )
        }
        onSirenStartCallback?.let {
            commands.add(
                VoiceCommand("siren_start", getCommand("siren_start", "siren aç"), it)
            )
        }
        onSirenStopCallback?.let {
            commands.add(
                VoiceCommand("siren_stop", getCommand("siren_stop", "siren kapat"), it)
            )
        }

        return commands.filter { it.phrase.isNotBlank() }
            .sortedByDescending { it.phrase.length }
    }

    private fun handleCommand(commandKey: String, action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()

        if (pendingCommand == commandKey &&
            now - pendingCommandTime <= confirmationWindowMs
        ) {
            pendingCommand = null
            pendingCommandTime = 0L

            Log.d("VoiceTriggerManager", "Komut doğrulandı: $commandKey")

            try {
                action()
            } catch (exception: Exception) {
                Log.e("VoiceTriggerManager", "Komut işlemi başarısız oldu.", exception)
            } finally {
                vibrate(longArrayOf(0, 100, 70, 100))
            }
        } else {
            pendingCommand = commandKey
            pendingCommandTime = now
            Log.d("VoiceTriggerManager", "İlk algılama; onay bekleniyor: $commandKey")
        }
    }

    private fun processRecognizedText(text: String) {
        val spoken = normalize(text)
        if (spoken.isBlank()) return

        Log.d("VoiceTriggerManager", "Algılanan: '$text' -> '$spoken'")

        val commands = getCommands()
        if (commands.isEmpty()) return

        val words = spoken.split(" ")
        var index = 0

        while (index < words.size) {
            var matched: VoiceCommand? = null
            var matchedWordCount = 0

            for (command in commands) {
                val commandWords = command.phrase.split(" ")
                val end = index + commandWords.size

                if (end <= words.size &&
                    words.subList(index, end) == commandWords
                ) {
                    matched = command
                    matchedWordCount = commandWords.size
                    break
                }
            }

            if (matched != null) {
                handleCommand(matched.key, matched.action)
                index += matchedWordCount
            } else {
                index++
            }
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

        if (isListening && speechRecognizer != null) return

        isListening = true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("VoiceTriggerManager", "Ses tanıma desteklenmiyor.")
            isListening = false
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        scheduleRestart(150)
    }

    private fun scheduleRestart(delayMs: Long = 250L) {
        if (!isListening || restartPending) return

        restartPending = true
        handler.postDelayed({
            restartPending = false
            if (!isListening) return@postDelayed

            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (exception: Exception) {
                Log.e("VoiceTriggerManager", "Dinleme başlatılamadı.", exception)
                scheduleRestart(700)
            }
        }, delayMs)
    }

    private fun createRecognitionListener() = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceTriggerManager", "Dinlemeye hazır.")
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.e("VoiceTriggerManager", "Ses tanıma hatası: $error")
            scheduleRestart(500)
        }

        override fun onResults(results: Bundle?) {
            try {
                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                if (matches != null && isListening) {
                    // En iyi sonuçtan başla; aynı metindeki tekrarları da işle.
                    for (recognizedText in matches) {
                        processRecognizedText(recognizedText)
                        if (recognizedText.isNotBlank()) break
                    }
                }
            } catch (exception: Exception) {
                Log.e("VoiceTriggerManager", "Sonuç işlenemedi.", exception)
            } finally {
                scheduleRestart(250)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun stopListening() {
        isListening = false
        pendingCommand = null
        pendingCommandTime = 0L

        handler.removeCallbacksAndMessages(null)
        restartPending = false

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (exception: Exception) {
            Log.e("VoiceTriggerManager", "Ses tanıma kapatılırken hata oluştu.", exception)
        }

        speechRecognizer = null
        recognizerIntent = null
    }
}
