package com.example.data.model

data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionTag: String? = null
)

enum class PersonalityType(val title: String, val description: String) {
    CHULBULI_WITTY(
        title = "Chulbuli & Witty (चुलबुली)",
        description = "Smart, humorous, playful, slightly naughty, cheerful best friend tone"
    ),
    SWEET_CARING(
        title = "Sweet & Caring (प्यारी और हमदर्द)",
        description = "Warm, deeply empathetic, comforting when you feel down, gentle voice"
    ),
    SMART_PRO(
        title = "Sharp & Confident (आत्मविश्वासी)",
        description = "Clever, fast-thinking, polite, highly articulate AI companion"
    )
}

enum class LanguagePreference(val displayName: String, val code: String) {
    HINGLISH("Hinglish (हिंदी + English)", "hi_IN"),
    HINDI("हिंदी (Hindi)", "hi_IN"),
    ENGLISH("English", "en_IN")
}

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

sealed class ActionCommand {
    data class OpenApp(val appName: String, val packageName: String? = null) : ActionCommand()
    data class SetAlarm(val hour: Int, val minute: Int, val message: String = "Alarm") : ActionCommand()
    data class SetTimer(val seconds: Int, val message: String = "Timer") : ActionCommand()
    data class PhoneDial(val number: String) : ActionCommand()
    data class SendSms(val number: String, val message: String) : ActionCommand()
    object CheckBattery : ActionCommand()
    object TellTimeDate : ActionCommand()
    object OpenWifiSettings : ActionCommand()
    object OpenBluetoothSettings : ActionCommand()
    data class WebSearch(val query: String) : ActionCommand()
    data class OpenYouTube(val searchQuery: String? = null) : ActionCommand()
    object None : ActionCommand()
}
