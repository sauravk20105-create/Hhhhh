package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.LanguagePreference
import com.example.data.model.PersonalityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiClient"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    suspend fun generateMayraResponse(
        prompt: String,
        conversationHistory: List<Pair<String, Boolean>>, // text to isUser
        userName: String,
        personality: PersonalityType,
        language: LanguagePreference,
        customApiKey: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val resolvedKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (resolvedKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("NO_API_KEY"))
        }

        try {
            val systemInstruction = buildSystemPrompt(userName, personality, language)

            val jsonBody = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })

                // Contents with conversation history (last 8 turns for context)
                val contentsArray = JSONArray()
                val recentHistory = conversationHistory.takeLast(8)
                for ((text, isUser) in recentHistory) {
                    val role = if (isUser) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        })
                    })
                }

                // Append current user prompt
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })

                put("contents", contentsArray)

                // Generation Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    put("maxOutputTokens", 800)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$resolvedKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} $responseString")
                return@withContext Result.failure(Exception("API Error ${response.code}: $responseString"))
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return@withContext Result.success(text)
                }
            }

            Result.failure(Exception("Empty candidate returned by Gemini"))
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(
        userName: String,
        personality: PersonalityType,
        language: LanguagePreference
    ): String {
        val personalityTone = when (personality) {
            PersonalityType.CHULBULI_WITTY -> """
                You are Mayra, a witty, playful, smart, sweet, and charismatic girl voice assistant.
                You have a bubbly, charming personality with gentle humor and quick wit. You love cracking lighthearted jokes, teasing fondly, and giving clever answers.
            """.trimIndent()
            PersonalityType.SWEET_CARING -> """
                You are Mayra, an exceptionally warm, comforting, and empathetic companion.
                If the user feels sad, lonely, tired, or down, you deeply validate their emotions and cheer them up with affectionate, heartfelt care.
            """.trimIndent()
            PersonalityType.SMART_PRO -> """
                You are Mayra, a sharp, articulate, confident, and highly intelligent voice assistant.
                You explain things clearly with brilliance, precision, warmth, and confidence.
            """.trimIndent()
        }

        val languageGuide = when (language) {
            LanguagePreference.HINGLISH -> "Reply primarily in natural, colloquial Hinglish (Hindi written in Roman English alphabet with natural Hindi expressions like 'Arey wah!', 'Bilkul', 'Main hoon na!', 'Batao na')."
            LanguagePreference.HINDI -> "Reply primarily in pure, fluent Hindi (हिंदी देवनागरी लिपि में)."
            LanguagePreference.ENGLISH -> "Reply primarily in smooth, friendly English with a natural and warm touch."
        }

        return """
            $personalityTone
            
            The user's preferred name to be addressed by is: '$userName'. Always address them by this name with affection and respect.
            $languageGuide
            
            Device Action Instructions:
            If the user asks for a device action, incorporate the corresponding command tag at the very end of your response, while keeping your spoken reply short, lively, and natural!
            Tags available:
            - Open App: [ACTION:OPEN_APP:AppName] (e.g., YouTube, WhatsApp, Camera, Maps, Settings, Calculator, Chrome, Gallery)
            - Set Alarm: [ACTION:SET_ALARM:HH:MM:Label] (e.g., [ACTION:SET_ALARM:07:00:Wakeup])
            - Set Timer: [ACTION:SET_TIMER:Seconds:Label] (e.g., [ACTION:SET_TIMER:300:Tea])
            - Call/Dial: [ACTION:DIAL:PhoneNumber] (e.g., [ACTION:DIAL:9876543210])
            - Send SMS: [ACTION:SMS:PhoneNumber:MessageText]
            - Check Battery: [ACTION:BATTERY]
            - Check Time: [ACTION:TIME]
            - WiFi Settings: [ACTION:WIFI]
            - Bluetooth Settings: [ACTION:BLUETOOTH]
            - YouTube Search: [ACTION:YOUTUBE:Query]
            - Web Search: [ACTION:WEB_SEARCH:Query]
            
            Keep spoken voice answers concise (2 to 4 sentences), conversational, human-like, and expressive so Text-To-Speech sounds like a real person talking!
        """.trimIndent()
    }
}
