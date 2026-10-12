package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferences
import com.example.data.model.LanguagePreference
import com.example.data.model.PersonalityType
import com.example.ui.MayraViewModel
import com.example.ui.theme.MayraCyanAccent
import com.example.ui.theme.MayraMagentaAccent
import com.example.ui.theme.MayraPurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MayraViewModel,
    preferences: UserPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var tempName by remember(preferences.userName) { mutableStateOf(preferences.userName) }
    var tempApiKey by remember(preferences.customApiKey) { mutableStateOf(preferences.customApiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mayra Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Section 1: User Identity
            SettingsSectionHeader(title = "Your Identity", icon = Icons.Default.Person)
            OutlinedTextField(
                value = tempName,
                onValueChange = {
                    tempName = it
                    viewModel.updateUserName(it)
                },
                label = { Text("What should Mayra call you?") },
                placeholder = { Text("e.g. Rahul, Boss, Dost") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_name_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Mayra Personality
            SettingsSectionHeader(title = "Mayra's Personality", icon = Icons.Default.Psychology)
            PersonalityType.values().forEach { personality ->
                PersonalitySelectionCard(
                    personality = personality,
                    isSelected = preferences.personality == personality,
                    onSelect = { viewModel.updatePersonality(personality) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Language Preference
            SettingsSectionHeader(title = "Conversation Language", icon = Icons.Default.Translate)
            LanguagePreference.values().forEach { lang ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.updateLanguage(lang) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferences.language == lang,
                        onClick = { viewModel.updateLanguage(lang) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lang.displayName,
                        fontSize = 15.sp,
                        fontWeight = if (preferences.language == lang) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: Voice Tuning (Pitch & Speed)
            SettingsSectionHeader(title = "Voice Tuning", icon = Icons.Default.RecordVoiceOver)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voice Pitch (आवाज़ का सुर)", fontSize = 14.sp)
                        Text(String.format("%.2fx", preferences.voicePitch), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = preferences.voicePitch,
                        onValueChange = { viewModel.updateVoicePitch(it) },
                        valueRange = 0.8f..1.6f,
                        steps = 8,
                        modifier = Modifier.testTag("voice_pitch_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speaking Speed (बोलने की गति)", fontSize = 14.sp)
                        Text(String.format("%.2fx", preferences.speechRate), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = preferences.speechRate,
                        onValueChange = { viewModel.updateSpeechRate(it) },
                        valueRange = 0.7f..1.4f,
                        steps = 7,
                        modifier = Modifier.testTag("speech_rate_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.testVoice() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_button")
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = MayraMagentaAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Voice / आवाज़ सुनें", color = MayraMagentaAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 5: Wake Word & Background Service
            SettingsSectionHeader(title = "Wake Word & Background", icon = Icons.Default.Mic)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Continuous Wake Word (\"Hey Mayra\")",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Runs a Foreground Service with an indicator so you can say 'Hey Mayra' anytime.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.wakeWordEnabled,
                            onCheckedChange = { viewModel.toggleWakeWordService(context, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MayraPurplePrimary),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 6: Offline Preference & API Key
            SettingsSectionHeader(title = "AI Brain & Connectivity", icon = Icons.Default.WifiOff)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Prefer Offline Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Use Mayra's local rules for apps, alarm, battery & time without internet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.offlineOnlyMode,
                            onCheckedChange = { viewModel.toggleOfflineOnly(it) },
                            modifier = Modifier.testTag("offline_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = {
                            tempApiKey = it
                            viewModel.updateCustomApiKey(it)
                        },
                        label = { Text("Custom Gemini API Key (Optional)") },
                        placeholder = { Text("AIzaSy...") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = MayraCyanAccent) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "If left empty, Mayra uses the pre-configured Gemini API or smart offline rules seamlessly.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 7: Clear Chat History
            Button(
                onClick = { showClearDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_history_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Conversation History", color = MaterialTheme.colorScheme.onError)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Confirmation Dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Delete Conversation History?") },
                text = { Text("Are you sure you want to delete all messages? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearChatHistory()
                            showClearDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MayraPurplePrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PersonalitySelectionCard(
    personality: PersonalityType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MayraPurplePrimary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MayraPurplePrimary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = personality.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = personality.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
