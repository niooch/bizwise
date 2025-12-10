package com.example.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.InformationAboutMe
import com.example.app.data.RetrofitClient

@Composable
fun UserScreen(
    token: String,
    onProfileClick: () -> Unit,
    onCoursesClick: () -> Unit
) {
    var userData by remember { mutableStateOf<InformationAboutMe?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.informationAboutMe("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                userData = response.body()
            }
        } catch (e: Exception) {
            //TODO: Obsługa błędu
        }
    }
    Scaffold(
        // DOLNY PASEK (K, Q, F)
        bottomBar = {
            NavigationBar {
                // Ikona K (np. Kursy)
                NavigationBarItem(
                    selected = false,
                    onClick = onCoursesClick,
                    icon = {
                        Text("K", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    },
                    label = { Text("Kursy") }
                )

                // Ikona Q
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO: Logika przejścia do Quizów (Q) */ },
                    icon = {
                        Text("Q", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    },
                    label = { Text("Quizy") }
                )

                // Ikona F
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO: Logika przejścia do Forum (F) */ },
                    icon = {
                        Text("F", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    },
                    label = { Text("Forum") }
                )
            }
        }
    ) { innerPadding ->
        // --- GŁÓWNA ZAWARTOŚĆ ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Ważne: uwzględnia miejsce zajęte przez dolny pasek
        ) {

            // --- PRAWY GÓRNY RÓG (Ikona P) ---
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .align(Alignment.TopEnd) // Przyklej do prawego górnego rogu
                    .padding(16.dp)
            ) {
                // Robię kółeczko, żeby wyglądało jak awatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("P", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- ŚRODEK (Powitanie) ---
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userData != null) {
                    // Jeśli mamy dane z JSONa
                    Text(
                        text = "Witaj ${userData!!.username}", //TODO: poprawić !!, to niebezpieczne
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Jeśli dane się jeszcze ładują lub jest błąd
                    Text("Witaj Studencie", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}