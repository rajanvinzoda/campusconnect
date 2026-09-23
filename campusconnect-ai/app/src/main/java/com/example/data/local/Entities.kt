package com.example.data.local

data class PostEntity(
    val id: String,
    val authorName: String,
    val authorRole: String,
    val department: String,
    val timestamp: Long,
    val type: String,
    val content: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val category: String = "General"
)

data class ChatMessageEntity(
    val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isVoiceNote: Boolean = false,
    val voiceDurationSec: Int = 0,
    val attachmentUrl: String? = null
)

data class ResourceEntity(
    val id: String,
    val title: String,
    val subject: String,
    val department: String,
    val fileType: String,
    val downloadsCount: Int = 0,
    val isDownloaded: Boolean = false
)
