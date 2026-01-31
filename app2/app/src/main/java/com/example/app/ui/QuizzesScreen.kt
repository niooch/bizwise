package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.RetrofitClient
import com.example.app.data.QuizListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesScreen(
    token: String,
    onBack: () -> Unit,
    onQuizClick: (quizId: Int, lessonId: Int) -> Unit
) {
    var quizzes by remember { mutableStateOf<List<QuizListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val response = RetrofitClient.api.getAllQuizzes("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                quizzes = response.body()!!
            } else {
                errorMessage = "Nie udało się załadować listy quizów"
            }
        } catch (e: Exception) {
            Log.e("QuizzesScreen", "Error: ${e.message}")
            errorMessage = "Błąd połączenia: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dostępne Quizy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            errorMessage != null -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            quizzes.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                Text("Brak dostępnych quizów")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quizzes) { quiz ->
                        QuizCard(
                            quiz = quiz,
                            // Jeśli API nie zwraca lesson_id, przekazujemy 0 (do poprawy na backendzie)
                            onClick = { onQuizClick(quiz.id, quiz.lesson_id ?: 0) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizCard(
    quiz: QuizListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nagroda: ${quiz.exp_weight} EXP",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Graj",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}