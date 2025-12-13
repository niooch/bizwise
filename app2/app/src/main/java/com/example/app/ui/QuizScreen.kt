package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    token: String,
    quizId: Int,
    lessonId: Int,
    onBack: () -> Unit
) {

    val scope = rememberCoroutineScope()

    // Stany danych
    var quizData by remember { mutableStateOf<Quizz?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var isSubmittingAnswer by remember { mutableStateOf(false) }

    var selectedOptionId by remember { mutableStateOf<Int?>(null) }
    var typedAnswer by remember { mutableStateOf("") }
    val allAnswers = remember { mutableStateListOf<Answer>() }

    // 1. Pobieranie quizu
    LaunchedEffect(quizId) {
        try {
            val response = RetrofitClient.api.quizzToLesson("Bearer $token", quizId)
            if (response.isSuccessful && response.body() != null) {
                quizData = response.body()
            } else {
                errorMessage = "Nie udało się pobrać quizu (Kod: ${response.code()})"
            }
        } catch (e: Exception) {
            Log.e("QUIZ", "Error: ${e.message}")
            errorMessage = "Błąd połączenia: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    fun resetSelection() {
        selectedOptionId = null
        typedAnswer = ""
    }

    fun submitAnswerAndNext(question: Question) {
        scope.launch {
            isSubmittingAnswer = true
            errorMessage = null

            try {
                val singleAnswer = if (question.question_type == "CLOSED") {
                    Answer(
                        question_id = question.id,
                        selected_option_id = selectedOptionId ?: 0,
                        numeric_answer = 0
                    )
                } else {
                    Answer(
                        question_id = question.id,
                        selected_option_id = 0,
                        numeric_answer = typedAnswer.toIntOrNull() ?: 0
                    )
                }
                allAnswers.add(singleAnswer)

                if (quizData != null && currentQuestionIndex < quizData!!.questions.size - 1) {
                    currentQuestionIndex++
                    resetSelection()
                } else {
                    val requestBody = Answers(answers = allAnswers.toList())
                    RetrofitClient.api.submitAnswear(
                        token = "Bearer $token",
                        quizId = quizId,
                        answers = requestBody
                    )

                    RetrofitClient.api.compleLesson(
                        token = "Bearer $token",
                        LessonId = lessonId
                    )
                    onBack()
                }

            } catch (e: Exception) {
                Log.e("QUIZ", "Submit error: ${e.message}")
                errorMessage = "Nie udało się wysłać odpowiedzi. Spróbuj ponownie."
            } finally {
                isSubmittingAnswer = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quizData?.name ?: "Quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Wyjdź")
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
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Wróć")
                        }
                    }
                }

                quizData != null && quizData!!.questions.isNotEmpty() -> {

                    val questions = quizData!!.questions
                    val currentQuestion = questions[currentQuestionIndex]
                    val progress = (currentQuestionIndex + 1).toFloat() / questions.size

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        )

                        Text("Pytanie ${currentQuestionIndex + 1}/${questions.size}", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = currentQuestion.content,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        if (currentQuestion.question_type == "CLOSED") {
                            currentQuestion.answer_options.forEach { option ->
                                val isSelected = (selectedOptionId == option.id)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable { selectedOptionId = option.id },
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(text = option.content, modifier = Modifier.padding(16.dp), fontSize = 16.sp)
                                }
                            }
                        } else if (currentQuestion.question_type == "OPEN") {
                            OutlinedTextField(
                                value = typedAnswer,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) typedAnswer = newValue
                                },
                                label = { Text("Wpisz liczbę") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tutaj też możemy wyświetlić błąd wysyłania (mały czerwony tekst nad przyciskiem)
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = Color.Red, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = { submitAnswerAndNext(currentQuestion) },
                            enabled = !isSubmittingAnswer && (selectedOptionId != null || typedAnswer.isNotEmpty()),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (isSubmittingAnswer) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                if (currentQuestionIndex == questions.size -1 ) {
                                    Text("Zakończ Quiz")
                                } else {
                                    Text("Następne pytanie")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}