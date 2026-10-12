package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.LanguagePreference
import com.example.data.model.PersonalityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mayra_settings")

data class UserPreferences(
    val userName: String,
    val personality: PersonalityType,
    val language: LanguagePreference,
    val voicePitch: Float,
    val speechRate: Float,
    val wakeWordEnabled: Boolean,
    val offlineOnlyMode: Boolean,
    val customApiKey: String
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val PERSONALITY = stringPreferencesKey("personality")
        val LANGUAGE = stringPreferencesKey("language")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val OFFLINE_ONLY_MODE = booleanPreferencesKey("offline_only_mode")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.USER_NAME] ?: "Dost"
        val personalityName = prefs[Keys.PERSONALITY] ?: PersonalityType.CHULBULI_WITTY.name
        val personality = runCatching { PersonalityType.valueOf(personalityName) }
            .getOrDefault(PersonalityType.CHULBULI_WITTY)

        val languageName = prefs[Keys.LANGUAGE] ?: LanguagePreference.HINGLISH.name
        val language = runCatching { LanguagePreference.valueOf(languageName) }
            .getOrDefault(LanguagePreference.HINGLISH)

        val pitch = prefs[Keys.VOICE_PITCH] ?: 1.2f
        val rate = prefs[Keys.SPEECH_RATE] ?: 1.0f
        val wakeWord = prefs[Keys.WAKE_WORD_ENABLED] ?: true
        val offlineOnly = prefs[Keys.OFFLINE_ONLY_MODE] ?: false
        val customKey = prefs[Keys.CUSTOM_API_KEY] ?: ""

        UserPreferences(
            userName = name,
            personality = personality,
            language = language,
            voicePitch = pitch,
            speechRate = rate,
            wakeWordEnabled = wakeWord,
            offlineOnlyMode = offlineOnly,
            customApiKey = customKey
        )
    }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim() }
    }

    suspend fun updatePersonality(personality: PersonalityType) {
        context.dataStore.edit { it[Keys.PERSONALITY] = personality.name }
    }

    suspend fun updateLanguage(language: LanguagePreference) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateVoicePitch(pitch: Float) {
        context.dataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.5f, 2.0f) }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f) }
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun updateOfflineOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_ONLY_MODE] = enabled }
    }

    suspend fun updateCustomApiKey(key: String) {
        context.dataStore.edit { it[Keys.CUSTOM_API_KEY] = key.trim() }
    }
}
