package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.app.data.InformationAboutMe
import com.example.app.data.RetrofitClient

@Composable
fun UserScreen(
    token: String,
    onProfileClick: () -> Unit,
    onCoursesClick: () -> Unit,
    onQuizzesClick: () -> Unit,
    onForumClick: () -> Unit,
    onAuthExpired: () -> Unit
) {
    var userData by remember { mutableStateOf<InformationAboutMe?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        isLoading = true
        errorMessage = null
        try {
            val response = RetrofitClient.api.informationAboutMe("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                userData = response.body()
            } else if (response.code() == 401) {
                onAuthExpired()
            } else {
                errorMessage = "Nie udało się pobrać danych użytkownika (${response.code()})"
            }
        } catch (e: Exception) {
            Log.e("UserScreen", "Error: ${e.message}")
            errorMessage = "Błąd połączenia: ${e.localizedMessage ?: "nieznany"}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onCoursesClick,
                    icon = { Text("K", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    label = { Text("Kursy") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onQuizzesClick,
                    icon = { Text("Q", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    label = { Text("Quizy") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onForumClick,
                    icon = { Text("F", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    label = { Text("Forum") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                if (userData?.avatar?.image_url != null) {
                    AsyncImage(
                        model = userData!!.avatar!!.image_url,
                        contentDescription = "Profil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userData?.username?.take(1)?.uppercase() ?: "P",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    Text("Wczytywanie...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else if (userData != null) {
                    Text(
                        text = "Witaj ${userData!!.username}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("Witaj Studencie", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}