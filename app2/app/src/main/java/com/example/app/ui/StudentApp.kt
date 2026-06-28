package com.example.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.runtime.saveable.rememberSaveable

private const val AUTH_PREFS = "bizwise_auth"
private const val ACCESS_TOKEN_KEY = "access_token"
private const val REFRESH_TOKEN_KEY = "refresh_token"

@Composable
fun StudentApp() {
    val context = LocalContext.current
    val authPrefs = remember {
        context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
    }

    val savedAccessToken = remember { authPrefs.getString(ACCESS_TOKEN_KEY, null) }
    val savedRefreshToken = remember { authPrefs.getString(REFRESH_TOKEN_KEY, null) }

    var currentScreen by rememberSaveable {
        mutableStateOf(if (savedAccessToken != null) "USERSCREEN" else "WELCOME")
    }
    var userToken by rememberSaveable { mutableStateOf(savedAccessToken) }
    var logoutToken by rememberSaveable { mutableStateOf(savedRefreshToken) }
    var selectedCourseId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedLessonId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedQuizzId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedPostId by rememberSaveable { mutableStateOf<Int?>(null) }

    fun saveSession(accessToken: String, refreshToken: String) {
        authPrefs.edit()
            .putString(ACCESS_TOKEN_KEY, accessToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .apply()
    }

    fun clearSession() {
        authPrefs.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .apply()
    }

    fun logoutAndGoToWelcome() {
        userToken = null
        logoutToken = null
        selectedCourseId = null
        selectedLessonId = null
        selectedQuizzId = null
        selectedPostId = null
        clearSession()
        currentScreen = "WELCOME"
    }

    val systemBackEnabled = currentScreen in setOf(
        "LOGIN",
        "REGISTER",
        "PROFILE",
        "BADGES",
        "ALLCOURSES",
        "COURSEDETAILSCREEN",
        "LESSONS_SLIDE",
        "QUIZZ_SCREEN",
        "QUIZZES",
        "QUIZZ_SCREEN_FROM_LIST",
        "FORUM",
        "POST_DETAIL",
        "CREATE_POST"
    )

    BackHandler(enabled = systemBackEnabled) {
        currentScreen = when (currentScreen) {
            "LOGIN", "REGISTER" -> "WELCOME"
            "PROFILE", "ALLCOURSES", "QUIZZES", "FORUM" -> "USERSCREEN"
            "BADGES" -> "PROFILE"
            "COURSEDETAILSCREEN" -> "ALLCOURSES"
            "LESSONS_SLIDE", "QUIZZ_SCREEN" -> "COURSEDETAILSCREEN"
            "QUIZZ_SCREEN_FROM_LIST" -> "QUIZZES"
            "POST_DETAIL", "CREATE_POST" -> "FORUM"
            else -> currentScreen
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
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
                    saveSession(token, refresh)
                    currentScreen = "USERSCREEN"
                }
            )

            "REGISTER" -> RegisterScreen(
                onBack = { currentScreen = "WELCOME" }
            )

            "USERSCREEN" -> {
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
                        },
                        onAuthExpired = {
                            logoutAndGoToWelcome()
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
                            logoutAndGoToWelcome()
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
                            currentScreen = "QUIZZ_SCREEN"
                        }
                    )
                }
            }

            "QUIZZ_SCREEN" -> {
                if (userToken != null && selectedQuizzId != null && selectedLessonId != null) {
                    QuizScreen(
                        token = userToken!!,
                        lessonId = selectedLessonId!!,
                        quizId = selectedQuizzId!!,
                        onBack = { currentScreen = "COURSEDETAILSCREEN" }
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
