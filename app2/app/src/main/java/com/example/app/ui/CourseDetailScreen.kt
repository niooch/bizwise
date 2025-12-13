package com.example.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.RetrofitClient
import com.example.app.data.SingleLesson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(
    token: String,
    courseId: Int,
    onBack: () -> Unit,
    onLessonClick: (Int) -> Unit
) {
    var lessonsList by remember { mutableStateOf<List<SingleLesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pobieranie lekcji po wejściu na ekran
    LaunchedEffect(courseId) {
        try {
            // Wywołujemy endpoint: courses/{id}
            val response = RetrofitClient.api.singleLesson("Bearer $token", courseId)

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!

                // Sortujemy lekcje po 'order' żeby były w dobrej kolejności
                lessonsList = data.lessons.sortedBy { it.order }
            } else {
                errorMessage = "Błąd: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMessage = "Błąd sieci: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lekcje kursu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                errorMessage != null -> Text(text = errorMessage ?: "", modifier = Modifier.align(Alignment.Center))

                lessonsList.isEmpty() -> Text("Brak lekcji w tym kursie", modifier = Modifier.align(Alignment.Center))

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lessonsList) { lesson ->
                            LessonItem(
                                lesson = lesson,
                                onClick = { onLessonClick(lesson.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonItem(lesson: SingleLesson, onClick: () -> Unit) {
    // Ustalamy kolor i ikonę w zależności od statusu lekcji
    val (icon, tint) = when {
        lesson.locked -> Icons.Default.Lock to Color.Gray
        lesson.completed -> Icons.Default.CheckCircle to Color.Green
        else -> Icons.Default.PlayArrow to MaterialTheme.colorScheme.primary
    }

    Card(
        // Jeśli lekcja jest zablokowana, nie pozwalamy klikać (opcjonalnie)
        onClick = onClick,
        enabled = !lesson.locked,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (lesson.locked) Color.LightGray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lekcja ${lesson.order}: ${lesson.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
    }
}