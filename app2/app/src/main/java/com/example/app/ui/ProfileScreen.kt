package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.app.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    token: String,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onBadgesClick: () -> Unit
) {
    var userData by remember { mutableStateOf<InformationAboutMe?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }
    var availableAvatars by remember { mutableStateOf<List<Avatar>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Funkcja pobierająca profil (czysta coroutine)
    suspend fun fetchProfile() {
        try {
            val response = RetrofitClient.api.informationAboutMe("Bearer $token")
            if (response.isSuccessful) {
                userData = response.body()
                Log.d("API", "Profil pobrany: ${userData?.username}")
            }
        } catch (e: Exception) {
            Log.e("API", "Błąd pobierania: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        fetchProfile()
        try {
            val response = RetrofitClient.api.getAvatars("Bearer $token")
            if (response.isSuccessful) availableAvatars = response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("API", "Błąd awatarów: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mój Profil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Wstecz") }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading && userData == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (userData != null) {
                val user = userData!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- SEKCJA AWATARA ---
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { showSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // Sprawdzamy obiekt avatar zamiast stringa
                        if (user.avatar == null || user.avatar.image_url.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Red),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.username.take(1).uppercase(),
                                    color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            AsyncImage(
                                model = user.avatar.image_url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    TextButton(onClick = { showSheet = true }) {
                        Text("Zmień zdjęcie profilowe")
                    }

                    Text(text = user.username, fontSize = 32.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Karta EXP
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PUNKTY EXP", fontSize = 14.sp)
                            Text(text = user.exp.toString(), fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Karta STREAK
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            val currentStreak = user.streak.current_streak
                            Text("🔥 Streak: $currentStreak ${if (currentStreak == 1) "dzień" else "dni"}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onBadgesClick, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("ZOBACZ ODZNAKI") }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("WYLOGUJ SIĘ")
                    }
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                    Text("Wybierz awatar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(availableAvatars) { avatar ->
                            AsyncImage(
                                model = avatar.image_url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (userData?.avatar?.id == avatar.id) 4.dp else 1.dp,
                                        color = if (userData?.avatar?.id == avatar.id) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        scope.launch {
                                            val res = RetrofitClient.api.updateAvatar("Bearer $token", AvatarUpdate(avatar.id))
                                            if (res.isSuccessful) {
                                                fetchProfile()
                                                showSheet = false
                                            }
                                        }
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}