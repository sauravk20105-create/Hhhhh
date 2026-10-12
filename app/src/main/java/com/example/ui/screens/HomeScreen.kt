package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.MayraViewModel
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.MayraVoiceOrb
import com.example.ui.components.QuickSuggestionsRow
import com.example.ui.theme.MayraCyanAccent
import com.example.ui.theme.MayraMagentaAccent
import com.example.ui.theme.MayraPurpleDark
import com.example.ui.theme.MayraPurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MayraViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val liveRms by viewModel.liveRmsLevel.collectAsState()
    val liveTranscription by viewModel.liveTranscription.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState()

    var showTextInput by remember { mutableStateOf(false) }
    var textInputQuery by remember { mutableStateOf("") }
    var micPermissionGranted by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Permission launcher for Microphone and Notification
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micPermissionGranted = granted
        if (granted) {
            viewModel.startListening()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING)
                                        MayraMagentaAccent
                                    else
                                        MayraCyanAccent
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mayra AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (preferences.offlineOnlyMode) "Offline Brain Active" else "Online Gemini AI",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Quick suggestions
                QuickSuggestionsRow(
                    onSelect = { query ->
                        viewModel.processUserInput(query)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom control dock
                if (showTextInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInputQuery,
                            onValueChange = { textInputQuery = it },
                            placeholder = { Text("Ask Mayra anything...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_query_input"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MayraPurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (textInputQuery.isNotBlank()) {
                                    viewModel.processUserInput(textInputQuery)
                                    textInputQuery = ""
                                    showTextInput = false
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MayraPurplePrimary)
                                .testTag("send_query_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                        IconButton(
                            onClick = { showTextInput = false },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showTextInput = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("keyboard_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Type with keyboard",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Main Glowing Voice FAB
                        FloatingActionButton(
                            onClick = {
                                if (assistantState == AssistantState.SPEAKING) {
                                    viewModel.stopSpeaking()
                                } else if (assistantState == AssistantState.LISTENING) {
                                    viewModel.stopListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            containerColor = when (assistantState) {
                                AssistantState.LISTENING -> MayraCyanAccent
                                AssistantState.SPEAKING -> MayraMagentaAccent
                                else -> MayraPurplePrimary
                            },
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(listOf(MayraPurplePrimary, MayraMagentaAccent, MayraCyanAccent)),
                                    shape = CircleShape
                                )
                                .testTag("voice_fab_button")
                        ) {
                            Icon(
                                imageVector = when (assistantState) {
                                    AssistantState.LISTENING -> Icons.Default.Stop
                                    AssistantState.SPEAKING -> Icons.Default.Stop
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = "Voice input",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Speaking stop button placeholder / indicator
                        if (assistantState == AssistantState.SPEAKING) {
                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop speaking", tint = MayraMagentaAccent)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Central Orb Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MayraVoiceOrb(
                        state = assistantState,
                        rmsLevel = liveRms,
                        onClick = {
                            if (assistantState == AssistantState.SPEAKING) {
                                viewModel.stopSpeaking()
                            } else if (assistantState == AssistantState.LISTENING) {
                                viewModel.stopListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Text
                    Text(
                        text = when (assistantState) {
                            AssistantState.LISTENING -> "Mayra is listening... (बोलिए)"
                            AssistantState.THINKING -> "Thinking..."
                            AssistantState.SPEAKING -> "Mayra is speaking... (बोल रही है)"
                            AssistantState.ERROR -> "Something went wrong"
                            AssistantState.IDLE -> "Tap orb or say \"Hey Mayra\""
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (assistantState) {
                            AssistantState.LISTENING -> MayraCyanAccent
                            AssistantState.SPEAKING -> MayraMagentaAccent
                            AssistantState.THINKING -> MayraPurplePrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    // Live Speech Preview
                    AnimatedVisibility(
                        visible = liveTranscription.isNotBlank() && assistantState == AssistantState.LISTENING,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "\"$liveTranscription\"",
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Chat Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("chat_messages_list")
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onSpeakClick = { textToSpeak ->
                            viewModel.speakMessage(textToSpeak)
                        }
                    )
                }
            }
        }
    }
}
