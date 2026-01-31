package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.app.data.Badge
import com.example.app.data.InformationAboutMe
import com.example.app.data.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    token: String,
    onBack: () -> Unit
) {
    var userData by remember { mutableStateOf<InformationAboutMe?>(null) }
    var badges by remember { mutableStateOf<List<Badge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val userResponse = RetrofitClient.api.informationAboutMe("Bearer $token")
            val badgesResponse = RetrofitClient.api.getMyBadges("Bearer $token")

            if (userResponse.isSuccessful && badgesResponse.isSuccessful) {
                userData = userResponse.body()
                badges = badgesResponse.body() ?: emptyList()
            } else {
                errorMessage = "Błąd serwera: ${badgesResponse.code()}"
            }
        } catch (e: Exception) {
            Log.e("BadgesScreen", "Error: ${e.message}")
            errorMessage = "Błąd połączenia."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moje odznaki", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMessage != null -> Text(errorMessage!!, Modifier.align(Alignment.Center), color = Color.Red)
                badges.isEmpty() -> Text("Brak odznak.", Modifier.align(Alignment.Center), color = Color.Gray)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        userData?.let {
                            item {
                                UserHeader(it)
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }

                        items(badges) { badge ->
                            BadgeCard(badge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserHeader(user: InformationAboutMe) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(user.username.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(user.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Aktywny student", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun BadgeCard(badge: Badge) {
    // ANALOGIA DO SLAJDÓW: Identyczne sprawdzenie obecności obrazka
    val hasImage = !badge.image_url.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ANALOGIA DO SLAJDÓW: Renderowanie AsyncImage
            if (hasImage) {
                AsyncImage(
                    model = badge.image_url, // Przekazujemy URL bezpośrednio, jak w slajdach
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit // Identycznie jak w slajdach
                )
            } else {
                // Miejsce na ikonę zastępczą, jeśli URL jest pusty
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏅", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Zdobyto: ${badge.awarded_at.substringBefore("T")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}