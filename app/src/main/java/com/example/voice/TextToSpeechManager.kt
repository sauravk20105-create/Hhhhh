package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.LanguagePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch: Float = 1.2f
    private var currentRate: Float = 1.0f
    private var currentLanguage: LanguagePreference = LanguagePreference.HINGLISH

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyConfiguration()
                setupListener()
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                Log.e(TAG, "TTS Utterance error: $errorCode")
            }
        })
    }

    private fun applyConfiguration() {
        val engine = tts ?: return
        if (!isInitialized) return

        engine.setPitch(currentPitch)
        engine.setSpeechRate(currentRate)

        val locale = when (currentLanguage) {
            LanguagePreference.HINDI -> Locale("hi", "IN")
            LanguagePreference.HINGLISH -> {
                // Check if Hindi is available, else fallback to Indian English
                val hiResult = engine.isLanguageAvailable(Locale("hi", "IN"))
                if (hiResult >= TextToSpeech.LANG_AVAILABLE) Locale("hi", "IN") else Locale("en", "IN")
            }
            LanguagePreference.ENGLISH -> Locale("en", "IN")
        }

        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
    }

    fun updateSettings(pitch: Float, rate: Float, language: LanguagePreference) {
        currentPitch = pitch
        currentRate = rate
        currentLanguage = language
        applyConfiguration()
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not ready yet")
            return
        }

        // Clean any metadata brackets or markdown
        val cleanText = text
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .replace(Regex("\\*\\*|\\*"), "")
            .trim()

        if (cleanText.isBlank()) return

        stop()
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
