package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.model.LanguagePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String, Boolean) -> Unit, // text, isWakeWordTriggered
    private val onPartial: (String) -> Unit = {}
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    var continuousWakeWordMode: Boolean = false
    var currentLanguage: LanguagePreference = LanguagePreference.HINGLISH

    private var isRecognitionActive = false

    fun startListening() {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "Speech recognition not available on this device")
                return@post
            }

            if (speechRecognizer == null) {
                initRecognizer()
            }

            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    val localeStr = when (currentLanguage) {
                        LanguagePreference.HINDI -> "hi-IN"
                        LanguagePreference.HINGLISH -> "hi-IN" // Picks up Hindi + English code-switching best
                        LanguagePreference.ENGLISH -> "en-IN"
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeStr)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                isRecognitionActive = true
                _isListening.value = true
                Log.d(TAG, "Started listening")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                isRecognitionActive = false
                speechRecognizer?.stopListening()
                _isListening.value = false
                _rmsLevel.value = 0f
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listener", e)
            }
        }
    }

    private fun initRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize -2dB to 10dB to a 0f - 1f range
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _rmsLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _rmsLevel.value = 0f
                    Log.w(TAG, "SpeechRecognizer error: $error")
                    isRecognitionActive = false
                    _isListening.value = false

                    // In continuous wake word mode, restart listening after a brief pause
                    if (continuousWakeWordMode) {
                        mainHandler.postDelayed({
                            if (continuousWakeWordMode && !isRecognitionActive) {
                                startListening()
                            }
                        }, 1200)
                    }
                }

                override fun onResults(results: Bundle?) {
                    _rmsLevel.value = 0f
                    isRecognitionActive = false
                    _isListening.value = false

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()?.trim() ?: ""

                    if (spokenText.isNotBlank()) {
                        val lower = spokenText.lowercase(Locale.ROOT)
                        val isWakeWord = lower.contains("hey mayra") ||
                                lower.contains("hay mayra") ||
                                lower.contains("mayra") ||
                                lower.contains("मायरा") ||
                                lower.contains("सुनो मायरा")

                        // Strip wake word prefix if present for clean intent handling
                        val cleanCommand = cleanSpokenWakeWord(spokenText)
                        onResult(cleanCommand.ifBlank { spokenText }, isWakeWord)
                    }

                    if (continuousWakeWordMode) {
                        mainHandler.postDelayed({
                            if (continuousWakeWordMode && !isRecognitionActive) {
                                startListening()
                            }
                        }, 800)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = partials?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        onPartial(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun cleanSpokenWakeWord(text: String): String {
        return text
            .replace(Regex("^(hey|hay|ok|hello|sun|suno)?\\s*mayra\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(सुनो\\s*)?मायरा\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun destroy() {
        mainHandler.post {
            continuousWakeWordMode = false
            speechRecognizer?.destroy()
            speechRecognizer = null
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }

    companion object {
        private const val TAG = "SpeechRecognizerManager"
    }
}
