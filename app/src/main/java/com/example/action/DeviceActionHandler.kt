package com.example.action

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

object DeviceActionHandler {

    private const val TAG = "DeviceActionHandler"

    fun openApp(context: Context, appName: String): Pair<Boolean, String> {
        val cleanName = appName.trim().lowercase()
        val pm = context.packageManager

        // Known package mapping
        val packageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "camera" to null, // Special media intent
            "settings" to null, // Settings intent
            "calculator" to "com.google.android.calculator",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "clock" to "com.google.android.deskclock",
            "calendar" to "com.google.android.calendar",
            "photos" to "com.google.android.apps.photos",
            "gallery" to "com.google.android.apps.photos"
        )

        try {
            if (cleanName.contains("camera")) {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return Pair(true, "Opening Camera")
            }

            if (cleanName.contains("settings") || cleanName.contains("setting")) {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return Pair(true, "Opening Settings")
            }

            // Direct package match
            for ((key, pkg) in packageMap) {
                if (pkg != null && cleanName.contains(key)) {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        return Pair(true, "Opening $appName")
                    }
                }
            }

            // Search by installed apps
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        return Pair(true, "Opening ${pm.getApplicationLabel(app)}")
                    }
                }
            }

            // Web fallback if app not installed (e.g. YouTube)
            if (cleanName.contains("youtube")) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                return Pair(true, "Opening YouTube in browser")
            }

            return Pair(false, "Could not find $appName installed on device")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app $appName", e)
            return Pair(false, "Error opening $appName: ${e.message}")
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, message: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Alarm set for %02d:%02d".format(hour, minute))
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm", e)
            Pair(false, "Failed to set alarm: ${e.message}")
        }
    }

    fun setTimer(context: Context, seconds: Int, message: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Timer set for $seconds seconds")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting timer", e)
            Pair(false, "Failed to set timer: ${e.message}")
        }
    }

    fun openDialer(context: Context, rawNumber: String): Pair<Boolean, String> {
        return try {
            val cleanNumber = rawNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opening dialer with $cleanNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening dialer", e)
            Pair(false, "Failed to open dialer: ${e.message}")
        }
    }

    fun sendSms(context: Context, rawNumber: String, messageText: String): Pair<Boolean, String> {
        return try {
            val cleanNumber = rawNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                putExtra("sms_body", messageText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opening SMS composer")
        } catch (e: Exception) {
            Log.e(TAG, "Error composing SMS", e)
            Pair(false, "Failed to open SMS: ${e.message}")
        }
    }

    fun getBatteryInfo(context: Context): Pair<Int, Boolean> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 50

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return Pair(batteryPct, isCharging)
    }

    fun openWifiSettings(context: Context): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opening Wi-Fi Settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Wi-Fi", e)
            Pair(false, "Could not open Wi-Fi settings")
        }
    }

    fun openBluetoothSettings(context: Context): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opening Bluetooth Settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Bluetooth", e)
            Pair(false, "Could not open Bluetooth settings")
        }
    }

    fun openYouTube(context: Context, query: String?): Pair<Boolean, String> {
        return try {
            if (!query.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Pair(true, "Searching '$query' on YouTube")
                } else {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(webIntent)
                    Pair(true, "Searching '$query' on YouTube")
                }
            } else {
                openApp(context, "youtube")
            }
        } catch (e: Exception) {
            Pair(false, "Error opening YouTube: ${e.message}")
        }
    }

    fun webSearch(context: Context, query: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Pair(true, "Searching '$query' on Google")
            } else {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(webIntent)
                Pair(true, "Searching '$query' on Google")
            }
        } catch (e: Exception) {
            Pair(false, "Error searching: ${e.message}")
        }
    }
}
