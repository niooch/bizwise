package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.ForumTag
import com.example.app.data.PostListItem
import com.example.app.data.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(
    token: String,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit,
    onCreatePostClick: () -> Unit
) {
    var posts by remember { mutableStateOf<List<PostListItem>>(emptyList()) }
    var tags by remember { mutableStateOf<List<ForumTag>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagId by remember { mutableStateOf<Int?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Load tags
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getForumTags("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                tags = response.body()!!
            }
        } catch (e: Exception) {
            Log.e("ForumScreen", "Error loading tags: ${e.message}")
        }
    }

    // Load posts
    LaunchedEffect(selectedTagId, searchQuery) {
        isLoading = true
        errorMessage = null
        try {
            val response = RetrofitClient.api.getForumPosts(
                token = "Bearer $token",
                tagId = selectedTagId,
                search = if (searchQuery.isNotBlank()) searchQuery else null
            )
            if (response.isSuccessful && response.body() != null) {
                posts = response.body()!!
            } else {
                errorMessage = "Nie udalo sie zaladowac postow"
            }
        } catch (e: Exception) {
            Log.e("ForumScreen", "Error loading posts: ${e.message}")
            errorMessage = "Blad polaczenia: ${e.message}"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Forum") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(Icons.Default.Search, contentDescription = "Szukaj")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePostClick) {
                Icon(Icons.Default.Add, contentDescription = "Nowy post")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            if (isSearchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Szukaj postow...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                )
            }

            // Tags filter
            if (tags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTagId == null,
                            onClick = { selectedTagId = null },
                            label = { Text("Wszystkie") }
                        )
                    }
                    items(tags) { tag ->
                        FilterChip(
                            selected = selectedTagId == tag.id,
                            onClick = {
                                selectedTagId = if (selectedTagId == tag.id) null else tag.id
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                posts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Brak postow")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(posts) { post ->
                            PostCard(
                                post = post,
                                onClick = { onPostClick(post.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: PostListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = post.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Author and date
            Text(
                text = "${post.author_nickname} - ${formatDate(post.creation_date)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Content preview
            Text(
                text = post.content,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags
            if (post.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(post.tags) { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag.name, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Komentarze: ${post.comments_count}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Reakcje: ${post.reactions_count}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        // Format: "2025-01-15T10:00:00Z" -> "2025-01-15"
        dateString.substringBefore("T")
    } catch (e: Exception) {
        dateString
    }
}
