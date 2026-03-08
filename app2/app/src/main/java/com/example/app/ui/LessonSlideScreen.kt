package com.example.app.ui

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.app.data.AllSlides
import com.example.app.data.RetrofitClient
import com.example.app.data.SingleSlide

@Composable
fun LessonSlidesScreen(
    token: String,
    lessonId: Int,
    onBack: () -> Unit,
    onQuizStart: (Int) -> Unit
) {
    var slidesData by remember { mutableStateOf<AllSlides?>(null) }
    var currentSlideIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val sortedSlides = remember(slidesData) {
        slidesData?.slides?.sortedBy { it.order } ?: emptyList()
    }

    // 1. POBIERANIE DANYCH LEKCJI
    LaunchedEffect(lessonId) {
        try {
            val response = RetrofitClient.api.allSlides("Bearer $token", lessonId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                slidesData = body
            }
        } catch (e: Exception) {
            Log.e("API", "Błąd sieci: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // 2. LOGIKA ZALICZANIA LEKCJI
    LaunchedEffect(currentSlideIndex) {
        if (!isLoading && sortedSlides.isNotEmpty() && currentSlideIndex >= sortedSlides.size) {
            try {
                RetrofitClient.api.compleLesson("Bearer $token", lessonId)
                Log.d("LessonSlidesScreen", "Lekcja $lessonId została pomyślnie zaliczona.")
            } catch (e: Exception) {
                Log.e("LessonSlidesScreen", "Błąd podczas zaliczania lekcji: ${e.message}")
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (sortedSlides.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = {
                        if (currentSlideIndex < sortedSlides.size)
                            (currentSlideIndex + 1) / sortedSlides.size.toFloat()
                        else 1f
                    },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- WARSTWA INTERAKCJI (NAWIGACJA LEWO/PRAWO) ---
            if (!isLoading && currentSlideIndex < sortedSlides.size) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentSlideIndex > 0) currentSlideIndex--
                            }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                currentSlideIndex++
                            }
                    )
                }
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Zamknij")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (sortedSlides.isEmpty() && slidesData != null) {
                Text("Ta lekcja nie ma slajdów.", modifier = Modifier.align(Alignment.Center))
            } else if (currentSlideIndex < sortedSlides.size) {
                val currentSlide = sortedSlides[currentSlideIndex]

                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "SlideAnimation",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp)
                ) { slide ->
                    // Wycentrowanie SlideContent wewnątrz AnimatedContent
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SlideContent(slide = slide)
                    }
                }

                Text(
                    text = "${currentSlideIndex + 1} / ${sortedSlides.size}",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )

            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Lekcja zakończona!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onQuizStart(slidesData?.quiz_id ?: 0) }
                    ) {
                        Text("Rozpocznij Quiz")
                    }
                }
            }
        }
    }
}

@Composable
fun SlideContent(slide: SingleSlide) {
    val hasImage = !slide.image_url.isNullOrBlank()
    // Dodajemy stan przewijania
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Włączamy pionowe przewijanie
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasImage) {
            AsyncImage(
                model = slide.image_url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 400.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        ChatBubble(text = slide.text_content)
    }
}

@Composable
fun ChatBubble(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp
        ),
        shadowElevation = 4.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}