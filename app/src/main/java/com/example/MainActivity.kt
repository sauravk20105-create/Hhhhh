package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MayraViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MayraTheme

enum class Screen {
    HOME,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MayraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MayraApp()
                }
            }
        }
    }
}

@Composable
fun MayraApp(viewModel: MayraViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val preferences by viewModel.userPreferences.collectAsState()

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
            )
        }
        Screen.SETTINGS -> {
            BackHandler {
                currentScreen = Screen.HOME
            }
            SettingsScreen(
                viewModel = viewModel,
                preferences = preferences,
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
