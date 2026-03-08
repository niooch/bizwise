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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    token: String,
    quizId: Int,
    lessonId: Int,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()


    var quizData by remember { mutableStateOf<Quizz?>(null) }
    var quizAnswers by remember { mutableStateOf<QuizAnswersResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var loadError by remember { mutableStateOf<String?>(null) }
    var submitError by remember { mutableStateOf<String?>(null) }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var isSubmittingAnswer by remember { mutableStateOf(false) }
    var isQuizFinished by remember { mutableStateOf(false) }

    var isFeedbackVisible by remember { mutableStateOf(false) }
    var isCurrentAnswerCorrect by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var correctAnswersCount by remember { mutableStateOf(0) }
    var scorePercent by remember { mutableStateOf(0) }

    var selectedOptionId by remember { mutableStateOf<Int?>(null) }
    var typedAnswer by remember { mutableStateOf("") }
    val allAnswers = remember { mutableStateListOf<Answer>() }

    fun resetSelection() {
        selectedOptionId = null
        typedAnswer = ""
        isFeedbackVisible = false
        submitError = null
    }

    fun isNumericAnswerCorrect(input: String, expectedPattern: String?): Boolean {
        val expected = expectedPattern?.trim() ?: return false
        val trimmedInput = input.trim()
        val expectedInt = expected.toIntOrNull()
        val inputInt = trimmedInput.toIntOrNull()
        return if (expectedInt != null && inputInt != null) {
            expectedInt == inputInt
        } else {
            trimmedInput == expected
        }
    }

    fun checkAnswer(question: Question) {
        val correctAnswer = quizAnswers?.questions?.find { it.id == question.id }

        if (question.question_type == "CLOSED") {
            val correctList = correctAnswer?.correct_answer_options ?: emptyList()
            isCurrentAnswerCorrect = correctList.any { it.id == selectedOptionId }

            val correctLabels = correctList.joinToString(", ") { it.content }
            feedbackMessage = if (isCurrentAnswerCorrect) {
                "Poprawna odpowiedź!"
            } else {
                "Nieprawidłowa odpowiedź. Poprawna: ${if (correctLabels.isNotBlank()) correctLabels else "-"}"
            }
        } else {
            val expectedPattern = correctAnswer?.correct_numeric_pattern
            isCurrentAnswerCorrect = isNumericAnswerCorrect(typedAnswer, expectedPattern)
            feedbackMessage = if (isCurrentAnswerCorrect) {
                "Poprawna odpowiedź!"
            } else {
                "Nieprawidłowa odpowiedź. Poprawna: ${expectedPattern ?: "-"}"
            }
        }
        isFeedbackVisible = true
    }

    fun isSubmittedAnswerCorrect(question: Question, answer: Answer): Boolean {
        val correctAnswer = quizAnswers?.questions?.find { it.id == question.id } ?: return false
        return if (question.question_type == "CLOSED") {
            correctAnswer.correct_answer_options.orEmpty().any { it.id == answer.selected_option_id }
        } else {
            val expectedPattern = correctAnswer.correct_numeric_pattern
            isNumericAnswerCorrect(answer.numeric_answer.toString(), expectedPattern)
        }
    }

    fun handleNext(question: Question) {
        if (isSubmittingAnswer) return

        scope.launch {
            val singleAnswer = if (question.question_type == "CLOSED") {
                Answer(question_id = question.id, selected_option_id = selectedOptionId ?: 0, numeric_answer = 0)
            } else {
                Answer(question_id = question.id, selected_option_id = 0, numeric_answer = typedAnswer.toIntOrNull() ?: 0)
            }

            if (allAnswers.none { it.question_id == singleAnswer.question_id }) {
                allAnswers.add(singleAnswer)
            }

            val isLastQuestion = quizData != null && currentQuestionIndex == quizData!!.questions.size - 1

            if (!isLastQuestion) {
                currentQuestionIndex++
                resetSelection()
            } else {
                isSubmittingAnswer = true
                submitError = null
                try {
                    val answersSnapshot = allAnswers.toList()
                    val questionsSnapshot = quizData?.questions.orEmpty()
                    correctAnswersCount = questionsSnapshot.count { question ->
                        val submittedAnswer = answersSnapshot.find { it.question_id == question.id } ?: return@count false
                        isSubmittedAnswerCorrect(question, submittedAnswer)
                    }
                    scorePercent = if (questionsSnapshot.isNotEmpty()) {
                        ((correctAnswersCount.toFloat() / questionsSnapshot.size.toFloat()) * 100f).roundToInt()
                    } else {
                        0
                    }

                    val requestBody = Answers(answers = answersSnapshot)
                    RetrofitClient.api.submitAnswear("Bearer $token", quizId, requestBody)

                    isFeedbackVisible = false
                    isQuizFinished = true
                } catch (e: Exception) {
                    Log.e("QuizScreen", "Submit error: ${e.message}")
                    submitError = "Błąd zapisu wyników: ${e.localizedMessage}"
                } finally {
                    isSubmittingAnswer = false
                }
            }
        }
    }

    LaunchedEffect(quizId) {
        isLoading = true
        loadError = null
        try {
            val quizResp = RetrofitClient.api.quizzToLesson("Bearer $token", quizId)
            val answersResp = RetrofitClient.api.getQuizAnswers("Bearer $token", quizId)

            if (quizResp.isSuccessful && answersResp.isSuccessful) {
                quizData = quizResp.body()
                quizAnswers = answersResp.body()
            } else {
                loadError = "Błąd API: Quiz(${quizResp.code()}), Odpowiedzi(${answersResp.code()})"
            }
        } catch (e: Exception) {
            loadError = "Błąd połączenia: ${e.message}"
            Log.e("QuizScreen", "Error", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(quizData?.name ?: "Quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Wyjdź") }
                }
            )
        },
        bottomBar = {
            // Panel widoczny tylko gdy nie skończyliśmy quizu
            if (isFeedbackVisible && !isQuizFinished) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isCurrentAnswerCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = feedbackMessage,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentAnswerCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 16.sp
                        )

                        if (submitError != null) {
                            Text(
                                text = submitError!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { handleNext(quizData!!.questions[currentQuestionIndex]) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmittingAnswer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrentAnswerCorrect) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                            )
                        ) {
                            if (isSubmittingAnswer) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (currentQuestionIndex == (quizData?.questions?.size ?: 0) - 1) "Zakończ" else "Dalej")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                loadError != null -> Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError!!, color = Color.Red, textAlign = TextAlign.Center)
                    Button(onClick = onBack, Modifier.padding(top = 16.dp)) { Text("Wróć") }
                }

                isQuizFinished -> Column(modifier = Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Quiz zakończony!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Twoje odpowiedzi zostały wysłane.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Wynik: $scorePercent% ($correctAnswersCount/${quizData?.questions?.size ?: 0} poprawnych)",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Powrót")
                    }
                }

                quizData != null -> {
                    val questions = quizData!!.questions
                    val currentQuestion = questions[currentQuestionIndex]
                    val progress = (currentQuestionIndex + 1).toFloat() / questions.size

                    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pytanie ${currentQuestionIndex + 1}/${questions.size}", color = Color.Gray, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = currentQuestion.content, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))

                        if (currentQuestion.question_type == "CLOSED") {
                            currentQuestion.answer_options.forEach { option ->
                                val isSelected = (selectedOptionId == option.id)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                        .clickable(enabled = !isFeedbackVisible) { selectedOptionId = option.id },
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(text = option.content, modifier = Modifier.padding(16.dp), fontSize = 16.sp)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = typedAnswer,
                                onValueChange = { if (it.all { c -> c.isDigit() }) typedAnswer = it },
                                label = { Text("Wpisz liczbę") },
                                enabled = !isFeedbackVisible,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!isFeedbackVisible) {
                            Button(
                                onClick = { checkAnswer(currentQuestion) },
                                enabled = selectedOptionId != null || typedAnswer.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(top = 32.dp).height(50.dp)
                            ) { Text("Sprawdź odpowiedź") }
                        }
                    }
                }
            }
        }
    }
}
