package com.example.app.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.*
import kotlinx.coroutines.launch

// Funkcja pomocnicza do zbierania wszystkich ID odpowiedzi (zagnieżdżonych komentarzy)
fun collectReplyIds(comments: List<CommentTree>): Set<Int> {
    val replyIds = mutableSetOf<Int>()
    fun collectFromReplies(replies: List<CommentTree>) {
        for (reply in replies) {
            replyIds.add(reply.id)
            collectFromReplies(reply.replies)
        }
    }
    for (comment in comments) {
        collectFromReplies(comment.replies)
    }
    return replyIds
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    token: String,
    postId: Int,
    onBack: () -> Unit
) {
    var post by remember { mutableStateOf<PostDetail?>(null) }
    var reactions by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var commentText by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<Int?>(null) }
    var isSendingComment by remember { mutableStateOf(false) }
    var currentUserNickname by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun loadPost() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.api.getPostDetail("Bearer $token", postId)
                if (response.isSuccessful && response.body() != null) {
                    post = response.body()!!
                } else {
                    errorMessage = "Nie udalo sie zaladowac posta"
                }

                // Load reactions
                val reactionsResponse = RetrofitClient.api.getPostReactions("Bearer $token", postId)
                if (reactionsResponse.isSuccessful && reactionsResponse.body() != null) {
                    reactions = reactionsResponse.body()!!.reactions
                }
            } catch (e: Exception) {
                Log.e("PostDetailScreen", "Error loading post: ${e.message}")
                errorMessage = "Blad polaczenia: ${e.message}"
            }
            isLoading = false
        }
    }

    fun deletePost() {
        scope.launch {
            isDeleting = true
            deleteError = null
            try {
                val response = RetrofitClient.api.deletePost("Bearer $token", postId)
                if (response.isSuccessful) {
                    showDeleteDialog = false
                    onBack()
                } else {
                    deleteError = "Nie udało się usunąć posta (kod ${response.code()})"
                }
            } catch (e: Exception) {
                deleteError = "Błąd usuwania: ${e.message}"
            } finally {
                isDeleting = false
            }
        }
    }

    fun sendComment() {
        if (commentText.isBlank()) return

        scope.launch {
            isSendingComment = true
            try {
                val request = CreateCommentRequest(
                    content = commentText,
                    parent_comment_id = replyingToCommentId
                )
                val response = RetrofitClient.api.addComment("Bearer $token", postId, request)
                if (response.isSuccessful) {
                    commentText = ""
                    replyingToCommentId = null
                    loadPost() // Reload post to show new comment
                }
            } catch (e: Exception) {
                Log.e("PostDetailScreen", "Error sending comment: ${e.message}")
            }
            isSendingComment = false
        }
    }

    fun reactToPost(reactionType: String) {
        scope.launch {
            try {
                val request = ReactionRequest(reaction_type = reactionType)
                RetrofitClient.api.reactToPost("Bearer $token", postId, request)
                // Reload reactions
                val reactionsResponse = RetrofitClient.api.getPostReactions("Bearer $token", postId)
                if (reactionsResponse.isSuccessful && reactionsResponse.body() != null) {
                    reactions = reactionsResponse.body()!!.reactions
                }
            } catch (e: Exception) {
                Log.e("PostDetailScreen", "Error reacting: ${e.message}")
            }
        }
    }

    LaunchedEffect(postId) {
        loadPost()
    }
    LaunchedEffect(token) {
        try {
            val meResponse = RetrofitClient.api.informationAboutMe("Bearer $token")
            if (meResponse.isSuccessful && meResponse.body() != null) {
                currentUserNickname = meResponse.body()!!.username
            }
        } catch (e: Exception) {
            Log.e("PostDetailScreen", "Error loading user: ${e.message}")
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Usunąć post?") },
            text = {
                Column {
                    Text("Tej operacji nie można cofnąć.")
                    if (deleteError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = deleteError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { deletePost() },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text("Usuń")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    val isAuthor = post?.author_nickname == currentUserNickname
                    if (isAuthor) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isDeleting
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Usuń post")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (replyingToCommentId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Odpowiadasz na komentarz",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { replyingToCommentId = null }) {
                                Text("Anuluj")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Napisz komentarz...") },
                            singleLine = true,
                            enabled = !isSendingComment
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { sendComment() },
                            enabled = commentText.isNotBlank() && !isSendingComment
                        ) {
                            if (isSendingComment) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Wyslij")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            post != null -> {
                // Filtruj tylko komentarze glowne (nie bedace odpowiedziami)
                val replyIds = collectReplyIds(post!!.comments)
                val topLevelComments = post!!.comments.filter { it.id !in replyIds }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Post content
                    item {
                        PostContent(
                            post = post!!,
                            reactions = reactions,
                            onReact = { reactToPost(it) }
                        )
                    }

                    // Comments section header
                    item {
                        Text(
                            text = "Komentarze (${topLevelComments.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Comments
                    if (topLevelComments.isEmpty()) {
                        item {
                            Text(
                                text = "Brak komentarzy. Badz pierwszy!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(topLevelComments) { comment ->
                            CommentItem(
                                comment = comment,
                                depth = 0,
                                onReply = { replyingToCommentId = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostContent(
    post: PostDetail,
    reactions: Map<String, Int>,
    onReact: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = post.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Author and date
            Text(
                text = "${post.author_nickname} - ${post.creation_date.substringBefore("T")}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = post.content,
                fontSize = 16.sp
            )

            // Tags
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reactions
            Text(
                text = "Reakcje:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReactionButtons(
                reactions = reactions,
                onReact = onReact
            )
        }
    }
}

@Composable
fun ReactionButtons(
    reactions: Map<String, Int>,
    onReact: (String) -> Unit
) {
    val reactionTypes = listOf(
        "LIKE" to "\uD83D\uDC4D",
        "LAUGH" to "\uD83D\uDE02",
        "SAD" to "\uD83D\uDE22",
        "ANGRY" to "\uD83D\uDE21"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(reactionTypes) { (type, label) ->
            val count = reactions[type] ?: 0
            OutlinedButton(
                onClick = { onReact(type) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$label ($count)",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: CommentTree,
    depth: Int,
    onReply: (Int) -> Unit
) {
    val maxDepth = 3
    val paddingStart = (depth * 16).coerceAtMost(maxDepth * 16).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (depth == 0)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Author and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = comment.author_nickname,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = comment.creation_date.substringBefore("T"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Content
                Text(
                    text = comment.content,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Reply button
                TextButton(
                    onClick = { onReply(comment.id) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Odpowiedz", fontSize = 12.sp)
                }
            }
        }

        // Nested replies
        if (comment.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            comment.replies.forEach { reply ->
                CommentItem(
                    comment = reply,
                    depth = depth + 1,
                    onReply = onReply
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
