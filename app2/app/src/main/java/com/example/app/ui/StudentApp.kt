package com.example.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlin.math.log

@Composable
fun StudentApp() {
    var currentScreen by remember { mutableStateOf("WELCOME") }
    var userToken by remember { mutableStateOf<String?>(null) }
    var logoutToken by remember { mutableStateOf<String?>(null) }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var selectedLessonId by remember { mutableStateOf<Int?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                        }
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
                            currentScreen = "COURSE_DETAILS" //TODO logika przejscia do quizu
                        }
                    )
                }
            }
        }
    }
}