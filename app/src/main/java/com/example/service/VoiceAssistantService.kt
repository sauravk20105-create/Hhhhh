package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.action.OfflineMayraBrain
import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.UserPreferencesRepository
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VoiceAssistantService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var speechRecognizerManager: SpeechRecognizerManager? = null
    private var ttsManager: TextToSpeechManager? = null

    companion object {
        private const val TAG = "VoiceAssistantService"
        const val CHANNEL_ID = "mayra_voice_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.mayra.START_LISTENING"
        const val ACTION_STOP = "com.example.mayra.STOP_LISTENING"

        var isServiceRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ttsManager = TextToSpeechManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        startInForeground()
        initContinuousListening()
        isServiceRunning = true

        return START_STICKY
    }

    private fun startInForeground() {
        val stopIntent = Intent(this, VoiceAssistantService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_desc))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop_listening),
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initContinuousListening() {
        if (speechRecognizerManager != null) return

        speechRecognizerManager = SpeechRecognizerManager(
            context = applicationContext,
            onResult = { query, isWakeWord ->
                handleVoiceTrigger(query, isWakeWord)
            }
        ).apply {
            continuousWakeWordMode = true
            startListening()
        }
    }

    private fun handleVoiceTrigger(query: String, isWakeWord: Boolean) {
        if (query.isBlank()) return

        serviceScope.launch {
            val prefsRepo = UserPreferencesRepository(applicationContext)
            val prefs = prefsRepo.userPreferencesFlow.first()
            val db = AppDatabase.getInstance(applicationContext)

            // Save user message
            db.chatDao().insertMessage(
                ChatEntity(
                    text = query,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Process query
            val reply = OfflineMayraBrain.processInput(
                query = query,
                context = applicationContext,
                userName = prefs.userName,
                personality = prefs.personality,
                language = prefs.language
            )

            // Save assistant reply
            db.chatDao().insertMessage(
                ChatEntity(
                    text = reply.spokenText,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Speak reply
            ttsManager?.updateSettings(prefs.voicePitch, prefs.speechRate, prefs.language)
            ttsManager?.speak(reply.spokenText)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mayra Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active microphone and wake-word listening status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        isServiceRunning = false
        speechRecognizerManager?.destroy()
        speechRecognizerManager = null
        ttsManager?.shutdown()
        ttsManager = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        speechRecognizerManager?.destroy()
        ttsManager?.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
