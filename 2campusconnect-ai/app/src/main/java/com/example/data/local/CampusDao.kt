package com.example.data.local

import kotlinx.coroutines.flow.Flow

interface CampusDao {
    suspend fun clearPosts()
    suspend fun clearResources()
    fun getAllPosts(): Flow<List<PostEntity>>
    suspend fun insertPosts(posts: List<PostEntity>)
    suspend fun updatePostLike(postId: String, isLiked: Boolean, delta: Int)
    suspend fun updatePostBookmark(postId: String, isBookmarked: Boolean)
    suspend fun insertMessage(message: ChatMessageEntity)
    suspend fun markResourceDownloaded(resourceId: String, isDownloaded: Boolean)
}
