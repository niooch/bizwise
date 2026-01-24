package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.data.CreatePostRequest
import com.example.app.data.ForumTag
import com.example.app.data.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    token: String,
    onBack: () -> Unit,
    onPostCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<ForumTag>>(emptyList()) }
    var selectedTagIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingTags by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Load tags
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getForumTags("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                tags = response.body()!!
            }
        } catch (e: Exception) {
            Log.e("CreatePostScreen", "Error loading tags: ${e.message}")
        }
        isLoadingTags = false
    }

    fun createPost() {
        if (title.isBlank() || content.isBlank()) {
            errorMessage = "Tytul i tresc sa wymagane"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = CreatePostRequest(
                    title = title,
                    content = content,
                    tag_ids = selectedTagIds.toList()
                )
                val response = RetrofitClient.api.createPost("Bearer $token", request)
                if (response.isSuccessful) {
                    onPostCreated()
                } else {
                    errorMessage = "Nie udalo sie utworzyc posta"
                }
            } catch (e: Exception) {
                Log.e("CreatePostScreen", "Error creating post: ${e.message}")
                errorMessage = "Blad polaczenia: ${e.message}"
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nowy post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { createPost() },
                        enabled = !isLoading && title.isNotBlank() && content.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Opublikuj")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tytul") },
                singleLine = true,
                enabled = !isLoading
            )

            // Content field
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                label = { Text("Tresc") },
                enabled = !isLoading
            )

            // Tags section
            Text(
                text = "Tagi (opcjonalnie)",
                style = MaterialTheme.typography.labelLarge
            )

            if (isLoadingTags) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (tags.isEmpty()) {
                Text(
                    text = "Brak dostepnych tagow",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags) { tag ->
                        FilterChip(
                            selected = selectedTagIds.contains(tag.id),
                            onClick = {
                                selectedTagIds = if (selectedTagIds.contains(tag.id)) {
                                    selectedTagIds - tag.id
                                } else {
                                    selectedTagIds + tag.id
                                }
                            },
                            label = { Text(tag.name) },
                            enabled = !isLoading
                        )
                    }
                }
            }

            // Selected tags count
            if (selectedTagIds.isNotEmpty()) {
                Text(
                    text = "Wybrano tagow: ${selectedTagIds.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}