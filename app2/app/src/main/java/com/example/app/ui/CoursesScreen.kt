package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.RetrofitClient
import com.example.app.data.singleCourse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    token: String,        // Klucz do API
    onBack: () -> Unit,    // Funkcja powrotu
    onCourseClick: (Int) -> Unit //Funkcja do przechodzenia wewnątrz kursu
) {
    // 1. Stany widoku
    var coursesList by remember { mutableStateOf<List<singleCourse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 2. Pobieranie danych (Side Effect)
    LaunchedEffect(Unit) {
        try {
            // Pamiętaj o "Bearer " przed tokenem!
            val response = RetrofitClient.api.allCourses("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                coursesList = response.body()!!
            } else {
                errorMessage = "Błąd serwera: ${response.code()}"
                Log.e("API", "Błąd kursów: ${response.code()}")
            }
        } catch (e: Exception) {
            errorMessage = "Błąd połączenia: ${e.localizedMessage}"
            Log.e("API", "Wyjątek: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // 3. UI
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Dostępne Kursy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Przypadek A: Ładowanie
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // Przypadek B: Błąd
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Nie udało się pobrać kursów", color = MaterialTheme.colorScheme.error)
                        Text(text = errorMessage ?: "", fontSize = 12.sp)
                    }
                }

                // Przypadek C: Pusta lista
                coursesList.isEmpty() -> {
                    Text(
                        text = "Brak dostępnych kursów",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 18.sp
                    )
                }

                // Przypadek D: Wyświetlamy listę!
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(coursesList) { course ->
                            CourseItem(
                                course = course,
                                onClick = { onCourseClick(course.id.toInt()) }
                                )
                        }
                    }
                }
            }
        }
    }
}

// Pomocniczy komponent do wyświetlania pojedynczego kafelka kursu
@Composable
fun CourseItem(
    course: singleCourse,
    onClick: () -> Unit
    ) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikona po lewej
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Tekst
            Column {
                Text(
                    text = course.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
