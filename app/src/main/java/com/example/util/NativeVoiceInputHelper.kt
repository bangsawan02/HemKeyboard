package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Native Android SDK SpeechRecognizer helper.
 * Provides Voice-to-Text input directly inside the keyboard service.
 */
class NativeVoiceInputHelper(
    private val context: Context,
    private val onTextRecognized: (text: String, isFinal: Boolean) -> Unit,
    private val onListeningStateChanged: (isListening: Boolean, errorMsg: String?) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isAvailable()) {
            onListeningStateChanged(false, "Pengenalan suara tidak didukung di perangkat ini.")
            return
        }

        try {
            stopListening()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningStateChanged(true, null)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        onListeningStateChanged(false, null)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorText = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Kesalahan audio mikrofon"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara yang terdeteksi"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan"
                            else -> "Gagal mengenali suara"
                        }
                        onListeningStateChanged(false, errorText)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        onListeningStateChanged(false, null)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onTextRecognized(matches[0], true)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onTextRecognized(matches[0], false)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onListeningStateChanged(false, e.localizedMessage)
        }
    }

    fun stopListening() {
        try {
            if (isListening) {
                speechRecognizer?.stopListening()
            }
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            onListeningStateChanged(false, null)
        } catch (_: Exception) {}
    }
}
