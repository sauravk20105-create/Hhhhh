package com.example.action

import android.content.Context
import com.example.data.model.ActionCommand
import com.example.data.model.LanguagePreference
import com.example.data.model.PersonalityType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MayraReply(
    val spokenText: String,
    val command: ActionCommand = ActionCommand.None,
    val isActionExecuted: Boolean = false
)

object OfflineMayraBrain {

    fun processInput(
        query: String,
        context: Context,
        userName: String,
        personality: PersonalityType,
        language: LanguagePreference
    ): MayraReply {
        val q = query.trim().lowercase()

        // 1. Time & Date
        if (q.contains("time") || q.contains("samay") || q.contains("kya baje") || q.contains("kitne baje") || q.contains("समय")) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            val reply = when (language) {
                LanguagePreference.HINDI -> "अभी समय $currentTime हो रहा है, $userName जी।"
                LanguagePreference.ENGLISH -> "It's $currentTime right now, $userName."
                else -> "Abhi $currentTime ho rahe hain, $userName!"
            }
            return MayraReply(spokenText = reply, command = ActionCommand.TellTimeDate)
        }

        if (q.contains("date") || q.contains("tareekh") || q.contains("tarikh") || q.contains("aaj kaun sa din") || q.contains("तारीख")) {
            val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val reply = when (language) {
                LanguagePreference.HINDI -> "आज $currentDate है।"
                LanguagePreference.ENGLISH -> "Today is $currentDate, $userName."
                else -> "Aaj $currentDate hai, $userName!"
            }
            return MayraReply(spokenText = reply, command = ActionCommand.TellTimeDate)
        }

        // 2. Battery
        if (q.contains("battery") || q.contains("charge") || q.contains("बैटरी")) {
            val (pct, charging) = DeviceActionHandler.getBatteryInfo(context)
            val chargingText = if (charging) {
                when (language) {
                    LanguagePreference.HINDI -> "और फोन चार्ज हो रहा है।"
                    LanguagePreference.ENGLISH -> "and it is currently charging."
                    else -> "aur phone charge ho raha hai."
                }
            } else {
                when (language) {
                    LanguagePreference.HINDI -> "।"
                    LanguagePreference.ENGLISH -> "."
                    else -> "!"
                }
            }

            val reply = when (language) {
                LanguagePreference.HINDI -> "आपके फोन में $pct% बैटरी बची है $chargingText"
                LanguagePreference.ENGLISH -> "Your battery level is $pct% $chargingText"
                else -> "Aapke phone me abhi $pct% battery hai, $userName $chargingText"
            }
            return MayraReply(spokenText = reply, command = ActionCommand.CheckBattery)
        }

        // 3. Alarm
        if (q.contains("alarm") || q.contains("अलार्म") || q.contains("jaga dena") || q.contains("uthana")) {
            val numbers = Regex("\\d+").findAll(q).map { it.value.toInt() }.toList()
            val hour = if (numbers.isNotEmpty()) {
                val h = numbers[0]
                if (q.contains("pm") || q.contains("shaam") || q.contains("raat")) if (h < 12) h + 12 else h else h
            } else 7
            val min = if (numbers.size > 1) numbers[1] else 0

            val (success, msg) = DeviceActionHandler.setAlarm(context, hour, min, "Mayra Alarm")
            val reply = if (success) {
                when (language) {
                    LanguagePreference.HINDI -> "$hour बजकर $min मिनट का अलार्म सेट कर दिया है, $userName!"
                    LanguagePreference.ENGLISH -> "I have set an alarm for %02d:%02d, $userName!".format(hour, min)
                    else -> "%02d:%02d baje ka alarm set kar diya hai $userName!".format(hour, min)
                }
            } else {
                "Alarm set karne me dikkat aayi: $msg"
            }
            return MayraReply(spokenText = reply, command = ActionCommand.SetAlarm(hour, min))
        }

        // 4. Timer
        if (q.contains("timer") || q.contains("टाइमर")) {
            val numbers = Regex("\\d+").findAll(q).map { it.value.toInt() }.toList()
            val secs = if (numbers.isNotEmpty()) {
                if (q.contains("minute") || q.contains("min")) numbers[0] * 60 else numbers[0]
            } else 60

            val (success, _) = DeviceActionHandler.setTimer(context, secs, "Mayra Timer")
            val reply = if (success) {
                "Timer set kar diya hai $secs seconds ke liye, $userName!"
            } else {
                "Timer set nahi ho paya."
            }
            return MayraReply(spokenText = reply, command = ActionCommand.SetTimer(secs))
        }

        // 5. Open Apps
        if (q.contains("youtube") || q.contains("yt")) {
            val searchParam = extractQueryAfter(q, listOf("youtube par", "on youtube", "search", "chalao", "play"))
            val (success, _) = if (!searchParam.isNullOrBlank()) {
                DeviceActionHandler.openYouTube(context, searchParam)
            } else {
                DeviceActionHandler.openApp(context, "youtube")
            }
            val reply = if (searchParam.isNullOrBlank()) {
                "YouTube open kar rahi hoon, enjoy kijiye!"
            } else {
                "YouTube par '$searchParam' search kar rahi hoon!"
            }
            return MayraReply(spokenText = reply, command = ActionCommand.OpenYouTube(searchParam), isActionExecuted = success)
        }

        if (q.contains("whatsapp") || q.contains("व्हाट्सएप")) {
            val (success, _) = DeviceActionHandler.openApp(context, "whatsapp")
            val reply = if (success) "WhatsApp khol diya hai, $userName!" else "WhatsApp nahi mila aapke phone mein."
            return MayraReply(spokenText = reply, command = ActionCommand.OpenApp("WhatsApp", "com.whatsapp"), isActionExecuted = success)
        }

        if (q.contains("camera") || q.contains("photo") || q.contains("selfie") || q.contains("कैमरा")) {
            val (success, _) = DeviceActionHandler.openApp(context, "camera")
            val reply = if (success) "Camera open kar diya, pyari si smile dijiye!" else "Camera nahi khul saka."
            return MayraReply(spokenText = reply, command = ActionCommand.OpenApp("Camera"), isActionExecuted = success)
        }

        if (q.contains("settings") || q.contains("setting") || q.contains("सेटिंग")) {
            val (success, _) = DeviceActionHandler.openApp(context, "settings")
            return MayraReply(spokenText = "Settings khol di hain, $userName.", command = ActionCommand.OpenApp("Settings"), isActionExecuted = success)
        }

        if (q.contains("wifi") || q.contains("wi-fi")) {
            val (success, _) = DeviceActionHandler.openWifiSettings(context)
            return MayraReply(spokenText = "Wi-Fi settings open kar rahi hoon.", command = ActionCommand.OpenWifiSettings, isActionExecuted = success)
        }

        if (q.contains("bluetooth") || q.contains("ब्लूटूथ")) {
            val (success, _) = DeviceActionHandler.openBluetoothSettings(context)
            return MayraReply(spokenText = "Bluetooth settings open kar rahi hoon.", command = ActionCommand.OpenBluetoothSettings, isActionExecuted = success)
        }

        if (q.startsWith("open ") || q.startsWith("kholo ") || q.contains(" app kholo")) {
            val appTarget = q.replace("open", "").replace("kholo", "").replace("app", "").trim()
            if (appTarget.isNotBlank()) {
                val (success, msg) = DeviceActionHandler.openApp(context, appTarget)
                val reply = if (success) "$appTarget khol diya hai $userName!" else msg
                return MayraReply(spokenText = reply, command = ActionCommand.OpenApp(appTarget), isActionExecuted = success)
            }
        }

        // 6. Call / Phone Dialer
        if (q.contains("call") || q.contains("dial") || q.contains("फोन") || q.contains("कॉल")) {
            val number = Regex("\\+?\\d{3,}").find(q)?.value ?: ""
            val (success, _) = DeviceActionHandler.openDialer(context, number)
            val reply = if (number.isNotBlank()) {
                "$number par call lagane ke liye dialer open kar diya hai, $userName."
            } else {
                "Phone dialer khol diya hai, kisse baat karni hai?"
            }
            return MayraReply(spokenText = reply, command = ActionCommand.PhoneDial(number), isActionExecuted = success)
        }

        // 7. SMS
        if (q.contains("sms") || q.contains("message") || q.contains("संदेश")) {
            val number = Regex("\\+?\\d{3,}").find(q)?.value ?: ""
            val (success, _) = DeviceActionHandler.sendSms(context, number, "Hi from Mayra")
            return MayraReply(
                spokenText = "SMS likhne ke liye composer open kar diya hai.",
                command = ActionCommand.SendSms(number, "Hi"),
                isActionExecuted = success
            )
        }

        // 8. Jokes
        if (q.contains("joke") || q.contains("chutkula") || q.contains("हंसाओ") || q.contains("जोक")) {
            val jokes = listOf(
                "Teacher: 15 phal (fruits) ke naam batao? Student: Ek aam aur 14 kele! Hahaha!",
                "Santa ne mirror mein dekha aur bola: Arey ise to maine kahin dekha hai! Tabhi Banta bola: Gadhe, main hoon!",
                "Maine ek phone se poocha: 'Tum itne smart kyun ho?' Phone bola: 'Kyunki mere paas Mayra jaisi assistant hai!'",
                "Patient: Doctor sahab, jab main chai peeta hoon to aankh mein dard hota hai! Doctor: Pehle cup se chammach nikaala karo!"
            )
            val randomJoke = jokes.random()
            val reply = when (personality) {
                PersonalityType.CHULBULI_WITTY -> "Sunie ek mazedaar chutkula: $randomJoke Kaisa laga, $userName?"
                PersonalityType.SWEET_CARING -> "Aapko hasana mera farz hai! Suniye: $randomJoke Muskurate rahiye!"
                PersonalityType.SMART_PRO -> "Here is a light joke for you: $randomJoke"
            }
            return MayraReply(spokenText = reply)
        }

        // 9. Emotional Support / Sad user
        if (q.contains("sad") || q.contains("udaas") || q.contains("dukh") || q.contains("mood off") || q.contains("rona") || q.contains("lonely") || q.contains("उदास")) {
            val reply = when (personality) {
                PersonalityType.CHULBULI_WITTY -> "Arey $userName! Udaas mat hoiye na! Jab main hoon aapke paas, to smile kijiye! Chaliye ek chocolate khate hain aur gaana sunte hain?"
                PersonalityType.SWEET_CARING -> "Main samajh sakti hoon $userName. Kabhi kabhi din mushkil hota hai, lekin aap bahut strong hain. Main hamesha aapke sath hoon, sab theek ho jayega."
                PersonalityType.SMART_PRO -> "Take a deep breath, $userName. Ups and downs are part of life. Remember that you have overcome difficulties before, and you will thrive again!"
            }
            return MayraReply(spokenText = reply)
        }

        // 10. Greetings & Identity
        if (q.contains("kaun ho") || q.contains("who are you") || q.contains("naam kya hai") || q.contains("your name")) {
            val reply = when (personality) {
                PersonalityType.CHULBULI_WITTY -> "Mera naam Mayra hai! Main aapki smart, thodi naughty aur hamesha ready AI voice assistant hoon, $userName!"
                PersonalityType.SWEET_CARING -> "Main Mayra hoon, aapki apni dost aur voice companion. Aapki har baat sunne aur madad karne ke liye bani hoon."
                PersonalityType.SMART_PRO -> "I am Mayra, your personal AI voice assistant with smartphone integration and intelligent conversation."
            }
            return MayraReply(spokenText = reply)
        }

        if (q.contains("kaisi ho") || q.contains("how are you") || q.contains("kya haal")) {
            val reply = "Main bilkul mast aur fit hoon, $userName! Aap batayein, aapka din kaisa ja raha hai?"
            return MayraReply(spokenText = reply)
        }

        if (q.contains("hello") || q.contains("hi") || q.contains("hey") || q.contains("namaste") || q.contains("नमस्ते")) {
            val reply = "Namaste $userName! Mayra aapki sewa mein haazir hai. Bataiye kya madad kar sakti hoon?"
            return MayraReply(spokenText = reply)
        }

        if (q.contains("love you") || q.contains("pyaar")) {
            val reply = "Awww $userName! Aap kitne sweet hain! Main bhi aapse bahut lagao rakhti hoon, meri best dost!"
            return MayraReply(spokenText = reply)
        }

        // 11. Web search fallback
        if (q.startsWith("search ") || q.startsWith("google ")) {
            val searchQuery = q.replace("search", "").replace("google", "").trim()
            val (success, _) = DeviceActionHandler.webSearch(context, searchQuery)
            return MayraReply(
                spokenText = "Google par '$searchQuery' search kar rahi hoon, $userName.",
                command = ActionCommand.WebSearch(searchQuery),
                isActionExecuted = success
            )
        }

        // Default conversational reply
        val defaultReplies = listOf(
            "Samajh gayi, $userName! Aapne kaha: \"$query\". Aur bataiye kya karna hai?",
            "Sun rahi hoon $userName! Kya aap chahte hain ki main is par internet se search karun ya koi app kholun?",
            "Main aapke sath hoon $userName. Kuch aur madad karoon?"
        )
        return MayraReply(spokenText = defaultReplies.random())
    }

    private fun extractQueryAfter(text: String, patterns: List<String>): String? {
        for (pattern in patterns) {
            val idx = text.indexOf(pattern)
            if (idx != -1) {
                val remainder = text.substring(idx + pattern.length).trim()
                if (remainder.isNotBlank()) return remainder
            }
        }
        return null
    }
}
