package com.example.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier

@Composable
fun StudentApp() {
    var currentScreen by remember { mutableStateOf("WELCOME") }
    var userToken by remember { mutableStateOf<String?>(null) }
    var logoutToken by remember { mutableStateOf<String?>(null) }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var selectedLessonId by remember { mutableStateOf<Int?>(null) }
    var selectedQuizzId by remember { mutableStateOf<Int?>(null) }
    var selectedPostId by remember { mutableStateOf<Int?>(null) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF6E9FF),
            Color(0xFFFFE6F5)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            when (currentScreen) {

            "WELCOME" -> WelcomeScreen(
                onLoginClick = { currentScreen = "LOGIN" },
                onRegisterClick = { currentScreen = "REGISTER" }
            )

            "LOGIN" -> LoginScreen(
                onBack = { currentScreen = "WELCOME" },
                onLoginSuccess = { token, refresh ->
                    userToken = token
                    logoutToken = refresh
                    currentScreen = "USERSCREEN"
                }
            )

            "REGISTER" -> RegisterScreen(
                onBack = { currentScreen = "WELCOME" }
            )

            "USERSCREEN" -> {
                // Zabezpieczenie przed nullem
                if (userToken != null) {
                    UserScreen(
                        token = userToken!!,
                        onProfileClick = {
                            currentScreen = "PROFILE"
                        },
                        onCoursesClick = {
                            currentScreen = "ALLCOURSES"
                        },
                        onQuizzesClick = {
                            currentScreen = "QUIZZES"
                        },
                        onForumClick = {
                            currentScreen = "FORUM"
                        }
                    )
                } else {
                    currentScreen = "WELCOME"
                }
            }

            "PROFILE" -> {
                if (userToken != null) {
                    ProfileScreen(
                        token = userToken!!,
                        onBack = {
                            currentScreen = "USERSCREEN"
                        },
                        onLogout = {
                            currentScreen = "WELCOME"
                        },
                        onBadgesClick = { currentScreen = "BADGES" }
                    )
                } else {
                    currentScreen = "WELCOME"
                }
            }

            "BADGES" -> {
                if (userToken != null) {
                    BadgesScreen(
                        token = userToken!!,
                        onBack = { currentScreen = "PROFILE" }
                    )
                } else {
                    currentScreen = "WELCOME"
                }
            }
            "ALLCOURSES" -> {
                if (userToken != null) {
                    CoursesScreen(
                        token = userToken!!,
                        onBack = {
                            currentScreen = "USERSCREEN"
                        },
                        onCourseClick = { clickedId ->
                            selectedCourseId = clickedId
                            currentScreen = "COURSEDETAILSCREEN"
                        }
                    )
                }
            }
            "COURSEDETAILSCREEN" -> {
                if (userToken != null && selectedCourseId != null) {
                    CourseDetailsScreen(
                        token = userToken!!,
                        courseId = selectedCourseId!!,
                        onBack = { currentScreen = "ALLCOURSES" },
                        onLessonClick = { clickedId ->
                            selectedLessonId = clickedId
                            currentScreen = "LESSONS_SLIDE"
                        }
                    )
                }
            }
            "LESSONS_SLIDE" -> {
                if (userToken != null && selectedLessonId != null) {
                    LessonSlidesScreen(
                        token = userToken!!,
                        lessonId = selectedLessonId!!,
                        onBack = { currentScreen = "COURSEDETAILSCREEN" },
                        onQuizStart = { quizId ->
                            selectedQuizzId = quizId
                            currentScreen = "QUIZZ_SCREEN" //TODO logika przejscia do quizu
                        }
                    )
                }
            }
            "QUIZZ_SCREEN" -> {
                if (userToken != null && selectedQuizzId != null) {
                    QuizScreen(
                        token = userToken!!,
                        lessonId = selectedLessonId!!,
                        quizId = selectedQuizzId!!,
                        onBack = { currentScreen = "COURSEDETAILSCREEN"}
                    )
                }
            }

            "QUIZZES" -> {
                if (userToken != null) {
                    QuizzesScreen(
                        token = userToken!!,
                        onBack = { currentScreen = "USERSCREEN" },
                        onQuizClick = { quizId, lessonId ->
                            selectedQuizzId = quizId
                            selectedLessonId = lessonId
                            currentScreen = "QUIZZ_SCREEN_FROM_LIST"
                        }
                    )
                }
            }

            "QUIZZ_SCREEN_FROM_LIST" -> {
                if (userToken != null && selectedQuizzId != null && selectedLessonId != null) {
                    QuizScreen(
                        token = userToken!!,
                        lessonId = selectedLessonId!!,
                        quizId = selectedQuizzId!!,
                        onBack = { currentScreen = "QUIZZES" }
                    )
                }
            }

            "FORUM" -> {
                if (userToken != null) {
                    ForumScreen(
                        token = userToken!!,
                        onBack = { currentScreen = "USERSCREEN" },
                        onPostClick = { postId ->
                            selectedPostId = postId
                            currentScreen = "POST_DETAIL"
                        },
                        onCreatePostClick = {
                            currentScreen = "CREATE_POST"
                        }
                    )
                }
            }

            "POST_DETAIL" -> {
                if (userToken != null && selectedPostId != null) {
                    PostDetailScreen(
                        token = userToken!!,
                        postId = selectedPostId!!,
                        onBack = { currentScreen = "FORUM" }
                    )
                }
            }

            "CREATE_POST" -> {
                if (userToken != null) {
                    CreatePostScreen(
                        token = userToken!!,
                        onBack = { currentScreen = "FORUM" },
                        onPostCreated = { currentScreen = "FORUM" }
                    )
                }
            }
        }
    }
}
