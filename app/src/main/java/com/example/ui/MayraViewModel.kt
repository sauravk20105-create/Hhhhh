package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.DeviceActionHandler
import com.example.action.OfflineMayraBrain
import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ActionCommand
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.LanguagePreference
import com.example.data.model.PersonalityType
import com.example.data.remote.GeminiClient
import com.example.service.VoiceAssistantService
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MayraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val prefsRepo = UserPreferencesRepository(application)
    private val geminiClient = GeminiClient()
    private val ttsManager = TextToSpeechManager(application)

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    private val _liveRmsLevel = MutableStateFlow(0f)
    val liveRmsLevel: StateFlow<Float> = _liveRmsLevel.asStateFlow()

    private val _lastActionFeedback = MutableStateFlow<String?>(null)
    val lastActionFeedback: StateFlow<String?> = _lastActionFeedback.asStateFlow()

    private val _isServiceActive = MutableStateFlow(VoiceAssistantService.isServiceRunning)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val userPreferences: StateFlow<UserPreferences> = prefsRepo.userPreferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserPreferences(
                userName = "Dost",
                personality = PersonalityType.CHULBULI_WITTY,
                language = LanguagePreference.HINGLISH,
                voicePitch = 1.2f,
                speechRate = 1.0f,
                wakeWordEnabled = true,
                offlineOnlyMode = false,
                customApiKey = ""
            )
        )

    private var speechRecognizerManager: SpeechRecognizerManager? = null

    init {
        initSpeechRecognizer()
        observeTtsSpeakingState()
        insertInitialWelcomeIfEmpty()
    }

    private fun initSpeechRecognizer() {
        speechRecognizerManager = SpeechRecognizerManager(
            context = getApplication(),
            onResult = { text, _ ->
                _assistantState.value = AssistantState.IDLE
                _liveTranscription.value = ""
                processUserInput(text)
            },
            onPartial = { partialText ->
                _liveTranscription.value = partialText
            }
        )

        // Observe RMS level from speech recognizer
        viewModelScope.launch {
            speechRecognizerManager?.rmsLevel?.collect { level ->
                if (_assistantState.value == AssistantState.LISTENING) {
                    _liveRmsLevel.value = level
                }
            }
        }
    }

    private fun observeTtsSpeakingState() {
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _assistantState.value = AssistantState.SPEAKING
                } else if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }
    }

    private fun insertInitialWelcomeIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = chatDao.getRecentMessages(1).size
            if (count == 0) {
                val welcome = "Namaste! Main hoon Mayra, aapki personal voice assistant. Mujhse baat kijiye, koi app khulwaiye ya 'Hey Mayra' bolkar kuch bhi poochiye!"
                chatDao.insertMessage(
                    ChatEntity(
                        text = welcome,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun startListening() {
        if (_assistantState.value == AssistantState.SPEAKING) {
            ttsManager.stop()
        }
        val currentPrefs = userPreferences.value
        speechRecognizerManager?.currentLanguage = currentPrefs.language
        speechRecognizerManager?.startListening()
        _assistantState.value = AssistantState.LISTENING
    }

    fun stopListening() {
        speechRecognizerManager?.stopListening()
        _assistantState.value = AssistantState.IDLE
        _liveRmsLevel.value = 0f
    }

    fun toggleListening() {
        if (_assistantState.value == AssistantState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _assistantState.value = AssistantState.IDLE
    }

    fun processUserInput(input: String) {
        val query = input.trim()
        if (query.isBlank()) return

        stopListening()
        _assistantState.value = AssistantState.THINKING

        viewModelScope.launch {
            val prefs = userPreferences.value

            // 1. Save user query in DB
            chatDao.insertMessage(
                ChatEntity(
                    text = query,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            // 2. Decide AI vs Offline Brain
            var spokenResponse: String
            var executedAction: ActionCommand = ActionCommand.None
            var actionTag: String? = null

            if (prefs.offlineOnlyMode) {
                val offlineReply = OfflineMayraBrain.processInput(
                    query = query,
                    context = getApplication(),
                    userName = prefs.userName,
                    personality = prefs.personality,
                    language = prefs.language
                )
                spokenResponse = offlineReply.spokenText
                executedAction = offlineReply.command
            } else {
                // Try Gemini AI first
                val history = chatMessages.value.takeLast(6).map { it.text to it.isUser }
                val geminiResult = geminiClient.generateMayraResponse(
                    prompt = query,
                    conversationHistory = history,
                    userName = prefs.userName,
                    personality = prefs.personality,
                    language = prefs.language,
                    customApiKey = prefs.customApiKey.ifBlank { null }
                )

                if (geminiResult.isSuccess) {
                    val rawText = geminiResult.getOrNull() ?: ""
                    val (extractedAction, cleanSpoken, tag) = parseAndExecuteActionTag(rawText)
                    spokenResponse = cleanSpoken
                    executedAction = extractedAction
                    actionTag = tag
                } else {
                    // Seamless offline fallback
                    val offlineReply = OfflineMayraBrain.processInput(
                        query = query,
                        context = getApplication(),
                        userName = prefs.userName,
                        personality = prefs.personality,
                        language = prefs.language
                    )
                    spokenResponse = offlineReply.spokenText
                    executedAction = offlineReply.command
                }
            }

            // 3. Save assistant message
            chatDao.insertMessage(
                ChatEntity(
                    text = spokenResponse,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    actionTag = actionTag
                )
            )

            // 4. Speak response via TTS
            ttsManager.updateSettings(prefs.voicePitch, prefs.speechRate, prefs.language)
            ttsManager.speak(spokenResponse)
        }
    }

    private fun parseAndExecuteActionTag(rawText: String): Triple<ActionCommand, String, String?> {
        val actionRegex = Regex("\\[ACTION:([^\\]]+)\\]")
        val match = actionRegex.find(rawText)
        val cleanText = rawText.replace(actionRegex, "").trim()

        if (match == null) {
            return Triple(ActionCommand.None, cleanText, null)
        }

        val tagContent = match.groupValues[1]
        val parts = tagContent.split(":")
        val actionType = parts.getOrNull(0) ?: ""

        val context = getApplication<Application>()
        var action: ActionCommand = ActionCommand.None

        when (actionType) {
            "OPEN_APP" -> {
                val app = parts.getOrNull(1) ?: "YouTube"
                DeviceActionHandler.openApp(context, app)
                action = ActionCommand.OpenApp(app)
            }
            "SET_ALARM" -> {
                val time = parts.getOrNull(1)?.split(":")
                val h = time?.getOrNull(0)?.toIntOrNull() ?: 7
                val m = time?.getOrNull(1)?.toIntOrNull() ?: 0
                val label = parts.getOrNull(2) ?: "Mayra Alarm"
                DeviceActionHandler.setAlarm(context, h, m, label)
                action = ActionCommand.SetAlarm(h, m, label)
            }
            "SET_TIMER" -> {
                val secs = parts.getOrNull(1)?.toIntOrNull() ?: 60
                val label = parts.getOrNull(2) ?: "Mayra Timer"
                DeviceActionHandler.setTimer(context, secs, label)
                action = ActionCommand.SetTimer(secs, label)
            }
            "DIAL" -> {
                val num = parts.getOrNull(1) ?: ""
                DeviceActionHandler.openDialer(context, num)
                action = ActionCommand.PhoneDial(num)
            }
            "SMS" -> {
                val num = parts.getOrNull(1) ?: ""
                val msg = parts.getOrNull(2) ?: "Hi"
                DeviceActionHandler.sendSms(context, num, msg)
                action = ActionCommand.SendSms(num, msg)
            }
            "BATTERY" -> {
                action = ActionCommand.CheckBattery
            }
            "TIME" -> {
                action = ActionCommand.TellTimeDate
            }
            "WIFI" -> {
                DeviceActionHandler.openWifiSettings(context)
                action = ActionCommand.OpenWifiSettings
            }
            "BLUETOOTH" -> {
                DeviceActionHandler.openBluetoothSettings(context)
                action = ActionCommand.OpenBluetoothSettings
            }
            "YOUTUBE" -> {
                val q = parts.getOrNull(1)
                DeviceActionHandler.openYouTube(context, q)
                action = ActionCommand.OpenYouTube(q)
            }
            "WEB_SEARCH" -> {
                val q = parts.getOrNull(1) ?: ""
                DeviceActionHandler.webSearch(context, q)
                action = ActionCommand.WebSearch(q)
            }
        }

        return Triple(action, cleanText, tagContent)
    }

    fun speakMessage(message: String) {
        val prefs = userPreferences.value
        ttsManager.updateSettings(prefs.voicePitch, prefs.speechRate, prefs.language)
        ttsManager.speak(message)
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearAllMessages()
            insertInitialWelcomeIfEmpty()
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch { prefsRepo.updateUserName(name) }
    }

    fun updatePersonality(personality: PersonalityType) {
        viewModelScope.launch { prefsRepo.updatePersonality(personality) }
    }

    fun updateLanguage(language: LanguagePreference) {
        viewModelScope.launch {
            prefsRepo.updateLanguage(language)
            ttsManager.updateSettings(
                userPreferences.value.voicePitch,
                userPreferences.value.speechRate,
                language
            )
        }
    }

    fun updateVoicePitch(pitch: Float) {
        viewModelScope.launch {
            prefsRepo.updateVoicePitch(pitch)
            ttsManager.updateSettings(
                pitch,
                userPreferences.value.speechRate,
                userPreferences.value.language
            )
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            prefsRepo.updateSpeechRate(rate)
            ttsManager.updateSettings(
                userPreferences.value.voicePitch,
                rate,
                userPreferences.value.language
            )
        }
    }

    fun testVoice() {
        val prefs = userPreferences.value
        val testText = when (prefs.language) {
            LanguagePreference.HINDI -> "नमस्ते ${prefs.userName}, यह मेरी आवाज़ का नमूना है।"
            LanguagePreference.ENGLISH -> "Hello ${prefs.userName}, this is how my voice sounds."
            LanguagePreference.HINGLISH -> "Hello ${prefs.userName}! Ye meri natural voice hai, kaisi lag rahi hoon?"
        }
        ttsManager.updateSettings(prefs.voicePitch, prefs.speechRate, prefs.language)
        ttsManager.speak(testText)
    }

    fun toggleOfflineOnly(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateOfflineOnlyMode(enabled) }
    }

    fun updateCustomApiKey(key: String) {
        viewModelScope.launch { prefsRepo.updateCustomApiKey(key) }
    }

    fun toggleWakeWordService(context: Context, enable: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWakeWordEnabled(enable)
            if (enable) {
                VoiceAssistantService.start(context)
                _isServiceActive.value = true
            } else {
                VoiceAssistantService.stop(context)
                _isServiceActive.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.destroy()
        ttsManager.shutdown()
    }
}
