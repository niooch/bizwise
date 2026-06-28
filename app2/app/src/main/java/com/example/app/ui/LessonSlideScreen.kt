package com.example.app.ui

import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.example.app.data.AllSlides
import com.example.app.data.RetrofitClient
import com.example.app.data.SingleSlide

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LessonSlidesScreen(
    token: String,
    lessonId: Int,
    onBack: () -> Unit,
    onQuizStart: (Int) -> Unit
) {
    var slidesData by remember { mutableStateOf<AllSlides?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLessonCompletionSent by remember(lessonId) { mutableStateOf(false) }

    val sortedSlides = remember(slidesData) {
        slidesData?.slides?.sortedBy { it.order } ?: emptyList()
    }
    val pageCount = remember(sortedSlides.size) {
        if (sortedSlides.isNotEmpty()) sortedSlides.size + 1 else 1
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(lessonId) {
        isLoading = true
        isLessonCompletionSent = false
        slidesData = null
        try {
            val response = RetrofitClient.api.allSlides("Bearer $token", lessonId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                slidesData = body
            } else {
                Log.e("LessonSlidesScreen", "Błąd API: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("API", "Błąd sieci: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(pagerState.currentPage, isLoading, sortedSlides.size, isLessonCompletionSent) {
        if (!isLoading && sortedSlides.isNotEmpty() && pagerState.currentPage >= sortedSlides.size && !isLessonCompletionSent) {
            try {
                RetrofitClient.api.compleLesson("Bearer $token", lessonId)
                Log.d("LessonSlidesScreen", "Lekcja $lessonId została pomyślnie zaliczona.")
                isLessonCompletionSent = true
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
                        if (pagerState.currentPage < sortedSlides.size) {
                            (pagerState.currentPage + 1) / sortedSlides.size.toFloat()
                        }
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
            } else if (sortedSlides.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) { page ->
                    if (page < sortedSlides.size) {
                        val slide = sortedSlides[page]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp, bottom = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SlideContent(slide = slide)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Lekcja zakończona!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            val quizId = slidesData?.quiz_id
                            if (quizId != null && quizId > 0) {
                                Button(onClick = { onQuizStart(quizId) }) {
                                    Text("Rozpocznij Quiz")
                                }
                            } else {
                                Text(
                                    "Ta lekcja nie ma przypisanego quizu.",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = if (pagerState.currentPage < sortedSlides.size) {
                        "${pagerState.currentPage + 1} / ${sortedSlides.size}"
                    } else {
                        "Koniec lekcji"
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    "Nie udało się pobrać slajdów lekcji.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SlideContent(slide: SingleSlide) {
    val hasImage = !slide.image_url.isNullOrBlank()
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
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

            ChatBubble(
                text = slide.text_content,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChatBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp
        ),
        shadowElevation = 4.dp
    ) {
        HtmlFormattedText(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun HtmlFormattedText(
    text: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()
    val normalizedText = remember(text) {
        text
            .replace("\\n", "\n")
            .replace("\n", "<br/>")
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 18f
                setLineSpacing(0f, 1.25f)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.text = HtmlCompat.fromHtml(
                normalizedText,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }
    )
}
