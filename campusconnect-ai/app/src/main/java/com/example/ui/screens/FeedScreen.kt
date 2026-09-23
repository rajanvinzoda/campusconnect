package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.PostType
import com.example.data.model.UserRole
import com.example.ui.components.RoleBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    onCreatePost: (content: String, type: PostType, category: String, hashtags: List<String>) -> Unit,
    onLikePost: (String) -> Unit,
    onBookmarkPost: (String) -> Unit,
    currentRole: UserRole,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var aiFeedEnabled by remember { mutableStateOf(true) }
    var showCreatePostModal by remember { mutableStateOf(false) }

    var postContent by remember { mutableStateOf("") }
    var postType by remember { mutableStateOf(PostType.TEXT) }
    var postCategory by remember { mutableStateOf("General") }
    var postHashtags by remember { mutableStateOf("Campus, AI") }

    val categories = listOf("All", "Announcements", "Clubs", "Research", "Events", "Polls")

    val filteredPosts = remember(posts, selectedCategory, aiFeedEnabled) {
        posts.filter { post ->
            if (selectedCategory == "All") true
            else post.category.equals(selectedCategory, ignoreCase = true) ||
                 (selectedCategory == "Polls" && post.type == PostType.POLL)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // AI Recommendation Banner & Category Filter Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Feed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gemini AI Smart Feed",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = aiFeedEnabled,
                            onCheckedChange = { aiFeedEnabled = it },
                            modifier = Modifier.testTag("toggle_ai_feed_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                leadingIcon = if (selectedCategory == cat) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("feed_filter_$cat")
                            )
                        }
                    }
                }
            }

            // Posts List
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DynamicFeed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No posts in this category yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Be the first student or faculty member to create an announcement!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyLazyPosts(
                    posts = filteredPosts,
                    onLike = onLikePost,
                    onBookmark = onBookmarkPost
                )
            }
        }

        // Floating Action Button to Create Post
        FloatingActionButton(
            onClick = { showCreatePostModal = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_post_fab")
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Post", fontWeight = FontWeight.Bold)
            }
        }

        // Create Post Modal Dialog
        if (showCreatePostModal) {
            AlertDialog(
                onDismissRequest = { showCreatePostModal = false },
                title = { Text("Create Campus Post") },
                text = {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = postType == PostType.TEXT,
                                onClick = { postType = PostType.TEXT },
                                label = { Text("Text") }
                            )
                            FilterChip(
                                selected = postType == PostType.ANNOUNCEMENT,
                                onClick = { postType = PostType.ANNOUNCEMENT },
                                label = { Text("Notice") }
                            )
                            FilterChip(
                                selected = postType == PostType.POLL,
                                onClick = { postType = PostType.POLL },
                                label = { Text("Poll") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = postContent,
                            onValueChange = { postContent = it },
                            placeholder = { Text("Share an academic announcement, research update, or club query...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("create_post_content_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = postHashtags,
                            onValueChange = { postHashtags = it },
                            label = { Text("Hashtags (comma separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (postContent.isNotBlank()) {
                                val tags = postHashtags.split(",").map { it.trim() }
                                onCreatePost(postContent, postType, postCategory, tags)
                                postContent = ""
                                showCreatePostModal = false
                            }
                        },
                        modifier = Modifier.testTag("submit_new_post_button")
                    ) {
                        Text("Publish Post")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePostModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun LazyLazyPosts(
    posts: List<Post>,
    onLike: (String) -> Unit,
    onBookmark: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(post = post, onLike = onLike, onBookmark = onBookmark)
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLike: (String) -> Unit,
    onBookmark: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author & Role Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoleBadge(role = post.authorRole)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${post.department}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Body Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )

            // Poll Options Rendering
            if (post.type == PostType.POLL && post.pollOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    post.pollOptions.forEachIndexed { idx, opt ->
                        val votes = post.pollVotes[idx] ?: 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(opt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$votes votes", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Hashtags
            if (post.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.hashtags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Footer Actions: Like, Comment, Bookmark, Share
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onLike(post.id) },
                        modifier = Modifier.testTag("like_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.likesCount}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment")
                    }
                    Text("${post.commentsCount}", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onBookmark(post.id) },
                        modifier = Modifier.testTag("bookmark_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
        }
    }
}
