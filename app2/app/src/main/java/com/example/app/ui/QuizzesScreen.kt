package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.app.data.singleCourse

data class QuizItem(
    val quizId: Int,
    val lessonId: Int,
    val lessonName: String,
    val courseName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesScreen(
    token: String,
    onBack: () -> Unit,
    onQuizClick: (quizId: Int, lessonId: Int) -> Unit
) {
    var quizzes by remember { mutableStateOf<List<QuizItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            // Get all courses
            val coursesResponse = RetrofitClient.api.allCourses("Bearer $token")
            if (coursesResponse.isSuccessful && coursesResponse.body() != null) {
                val courses = coursesResponse.body()!!
                val allQuizzes = mutableListOf<QuizItem>()

                // For each course, get lessons and find quizzes
                for (course in courses) {
                    try {
                        val courseDetailResponse = RetrofitClient.api.singleLesson(
                            "Bearer $token",
                            course.id.toInt()
                        )
                        if (courseDetailResponse.isSuccessful && courseDetailResponse.body() != null) {
                            val courseDetail = courseDetailResponse.body()!!

                            // For each lesson, check if it has a quiz
                            for (lesson in courseDetail.lessons) {
                                try {
                                    val lessonResponse = RetrofitClient.api.allSlides(
                                        "Bearer $token",
                                        lesson.id
                                    )
                                    if (lessonResponse.isSuccessful && lessonResponse.body() != null) {
                                        val lessonDetail = lessonResponse.body()!!
                                        if (lessonDetail.quiz_id != null) {
                                            allQuizzes.add(
                                                QuizItem(
                                                    quizId = lessonDetail.quiz_id,
                                                    lessonId = lesson.id,
                                                    lessonName = lesson.name,
                                                    courseName = course.name
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("QuizzesScreen", "Error loading lesson ${lesson.id}: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("QuizzesScreen", "Error loading course ${course.id}: ${e.message}")
                    }
                }

                quizzes = allQuizzes
            } else {
                errorMessage = "Nie udalo sie zaladowac kursow"
            }
        } catch (e: Exception) {
            Log.e("QuizzesScreen", "Error: ${e.message}")
            errorMessage = "Blad polaczenia: ${e.message}"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Quizy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ladowanie quizow...")
                    }
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            quizzes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Brak dostepnych quizow")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quizzes) { quiz ->
                        QuizCard(
                            quiz = quiz,
                            onClick = { onQuizClick(quiz.quizId, quiz.lessonId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizCard(
    quiz: QuizItem,
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = quiz.lessonName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = quiz.courseName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Rozpocznij quiz",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
