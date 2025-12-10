package com.example.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlin.math.log

@Composable
fun StudentApp() {
    var currentScreen by remember { mutableStateOf("WELCOME") }
    // Access token trzymamy tylko po to, żeby przekazać go do UserScreen/ProfileScreen
    var userToken by remember { mutableStateOf<String?>(null) }
    var logoutToken by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentScreen) {

            "WELCOME" -> WelcomeScreen(
                onLoginClick = { currentScreen = "LOGIN" },
                onRegisterClick = { currentScreen = "REGISTER" }
            )

            "LOGIN" -> LoginScreen(
                onBack = { currentScreen = "WELCOME" },
                onLoginSuccess = { token, refresh ->
                    userToken = token
                    logoutToken = refresh
                    currentScreen = "USERSCREEN"
                }
            )

            "REGISTER" -> RegisterScreen(
                onBack = { currentScreen = "WELCOME" }
            )

            "USERSCREEN" -> {
                // Zabezpieczenie przed nullem
                if (userToken != null) {
                    UserScreen(
                        token = userToken!!,
                        onProfileClick = {
                            currentScreen = "PROFILE"
                        }
                    )
                } else {
                    currentScreen = "WELCOME"
                }
            }

            "PROFILE" -> {
                if (userToken != null) {
                    ProfileScreen(
                        token = userToken!!,
                        onBack = {
                            currentScreen = "USERSCREEN"
                        },
                        onLogout = {
                            // TODO: Tu wstawisz swoją logikę wylogowania (API call z drugim tokenem)
                            currentScreen = "WELCOME"
                        }
                    )
                } else {
                    currentScreen = "WELCOME"
                }
            }
        }
    }
}