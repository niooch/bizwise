package com.example.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.app.data.InformationAboutMe
import com.example.app.data.RetrofitClient

data class BadgeInfo(
    val title: String,
    val description: String
)

private val placeholderBadges = listOf(
    BadgeInfo("Pierwsze kroki", "Ukończ swój pierwszy kurs"),
    BadgeInfo("Aktywny forumowicz", "Napisz pierwszy post na forum"),
    BadgeInfo("Quiz Master", "Zdobądź pełen wynik w quizie")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    token: String,
    onBack: () -> Unit
) {
    var userData by remember { mutableStateOf<InformationAboutMe?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.informationAboutMe("Bearer $token")
            if (response.isSuccessful) {
                userData = response.body()
            }
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moje odznaki") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        userData?.let {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = it.username.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = it.username,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Text(
                                        text = "Odznaki wkrótce będą widoczne na Twoim profilu.",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        placeholderBadges.forEach { badge ->
                            BadgeCard(badge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(badge.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(badge.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Status: w przygotowaniu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
