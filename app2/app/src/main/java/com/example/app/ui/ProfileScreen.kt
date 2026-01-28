package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.InformationAboutMe
import com.example.app.data.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    token: String,          // 1. Potrzebujemy tylko tokena
    onBack: () -> Unit,     // 2. Powrót
    onLogout: () -> Unit,   // 3. Wylogowanie
    onBadgesClick: () -> Unit
) {
    // Stan lokalny ekranu
    var userData by remember { mutableStateOf<InformationAboutMe?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.informationAboutMe("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                userData = response.body()
            } else {
                Log.e("API", "Błąd profilu: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("API", "Błąd sieci: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mój Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { innerPadding ->

        // GŁÓWNY KONTENER
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            if (isLoading) {
                // EKRAN ŁADOWANIA
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (userData != null) {
                // EKRAN WŁAŚCIWY (Gdy mamy dane)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. AWATAR
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(color = Color.Red, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userData!!.username.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. NAZWA
                    Text(text = userData!!.username, fontSize = 32.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. EXP
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EXP / LEVEL", fontSize = 14.sp, fontWeight = FontWeight.Light)
                            Text(text = userData!!.exp, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. STREAK
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("🔥 Best Streak: ", fontSize = 20.sp) //TODO zmienic z best streak na strerak
                             if (userData!!.streak.best_streak == 1) {
                                Text(text = "${userData!!.streak.best_streak} dzień", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text(text = "${userData!!.streak.best_streak} dni", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 5. ODZNAKI
                    Button(
                        onClick = onBadgesClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("ZOBACZ ODZNAKI", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. WYLOGUJ (Teraz działa!)
                    Button(
                        onClick = onLogout, // <--- Wywołujemy funkcję z góry
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("WYLOGUJ SIĘ", fontSize = 18.sp)
                    }
                }
            } else {
                // EKRAN BŁĘDU (Gdy nie udało się pobrać)
                Text("Nie udało się pobrać profilu", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
