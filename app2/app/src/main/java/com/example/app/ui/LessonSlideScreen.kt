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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.AllSlides
import com.example.app.data.RetrofitClient
import com.example.app.data.SingleSlide
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale


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

    // 2. Pobieranie danych
    LaunchedEffect(lessonId) {
        try {
            val response = RetrofitClient.api.allSlides("Bearer $token", lessonId)
            if (response.isSuccessful && response.body() != null) {
                slidesData = response.body()
            } else {
                Log.e("API", "Błąd slajdów: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("API", "Błąd sieci: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // 3. UI Główny
    Scaffold(
        // Pasek postępu na górze (opcjonalnie)
        topBar = {
            if (sortedSlides.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { (currentSlideIndex + 1) / sortedSlides.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Brak efektu "fali" przy klikaniu (czystszy wygląd)
                ) {
                    if (sortedSlides.isNotEmpty()) { //Gdy wyjdziemy poza zakres przechodzimy do quizw
                            currentSlideIndex++
                    }
                }
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Zamknij")
            }

            // Zawartość
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (sortedSlides.isEmpty() && slidesData != null) {
                Text("Ta lekcja nie ma slajdów.", modifier = Modifier.align(Alignment.Center))
            } else if (currentSlideIndex < sortedSlides.size) {
                val currentSlide = sortedSlides[currentSlideIndex]

                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "SlideAnimation",
                    modifier = Modifier.align(Alignment.Center) // Centrujemy wszystko
                ) { slide ->
                    SlideContent(slide = slide)
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
                        onClick = {
                            onQuizStart(slidesData?.quiz_id ?: 0)
                        }
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
    // Sprawdzamy czy jest obrazek (czy string nie jest pusty/null)
    val hasImage = !slide.image_url.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        ChatBubble(text = slide.text_content)

        // 2. OBRAZEK (Jeśli istnieje)
        if (hasImage) {
            Spacer(modifier = Modifier.height(24.dp))

            AsyncImage(
                model = slide.image_url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ChatBubble(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer, // Kolor dymka
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 4.dp,
            bottomEnd = 24.dp
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