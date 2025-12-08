package com.example.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun StudentApp() {
    var currentScreen by remember { mutableStateOf("WELCOME") }
    var userToken by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentScreen) {
            "WELCOME" -> WelcomeScreen(
                onLoginClick = { currentScreen = "LOGIN" },
                onRegisterClick = { currentScreen = "REGISTER" }
            )
            "LOGIN" -> LoginScreen(
                onBack = { currentScreen = "WELCOME" },

                onLoginSuccess = { token ->
                    userToken = token
                    currentScreen = "USERSCREEN"
                }
            )
            "REGISTER" -> RegisterScreen(
                onBack = { currentScreen = "WELCOME" }
            )
            "USERSCREEN" -> UserScreen(
                token = userToken!! //TODO: poprawić logike, !! niebezpieczne
            )
        }
    }
}
