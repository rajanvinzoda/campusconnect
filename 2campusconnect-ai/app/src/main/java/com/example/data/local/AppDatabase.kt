package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppDatabase private constructor() {

    private val daoImpl = object : CampusDao {
        private val mutex = Mutex()
        private val _postsFlow = MutableStateFlow<List<PostEntity>>(emptyList())
        private val messages = mutableListOf<ChatMessageEntity>()
        private val downloadedResources = mutableSetOf<String>()

        override suspend fun clearPosts() {
            mutex.withLock {
                _postsFlow.value = emptyList()
            }
        }

        override suspend fun clearResources() {
            mutex.withLock {
                downloadedResources.clear()
            }
        }

        override fun getAllPosts(): Flow<List<PostEntity>> {
            return _postsFlow.asStateFlow()
        }

        override suspend fun insertPosts(posts: List<PostEntity>) {
            mutex.withLock {
                val current = _postsFlow.value.toMutableList()
                for (newPost in posts) {
                    val index = current.indexOfFirst { it.id == newPost.id }
                    if (index >= 0) {
                        current[index] = newPost
                    } else {
                        current.add(0, newPost)
                    }
                }
                _postsFlow.value = current
            }
        }

        override suspend fun updatePostLike(postId: String, isLiked: Boolean, delta: Int) {
            mutex.withLock {
                val current = _postsFlow.value.map { p ->
                    if (p.id == postId) {
                        p.copy(
                            isLiked = isLiked,
                            likesCount = (p.likesCount + delta).coerceAtLeast(0)
                        )
                    } else p
                }
                _postsFlow.value = current
            }
        }

        override suspend fun updatePostBookmark(postId: String, isBookmarked: Boolean) {
            mutex.withLock {
                val current = _postsFlow.value.map { p ->
                    if (p.id == postId) {
                        p.copy(isBookmarked = isBookmarked)
                    } else p
                }
                _postsFlow.value = current
            }
        }

        override suspend fun insertMessage(message: ChatMessageEntity) {
            mutex.withLock {
                messages.add(message)
            }
        }

        override suspend fun markResourceDownloaded(resourceId: String, isDownloaded: Boolean) {
            mutex.withLock {
                if (isDownloaded) {
                    downloadedResources.add(resourceId)
                } else {
                    downloadedResources.remove(resourceId)
                }
            }
        }
    }

    fun campusDao(): CampusDao = daoImpl

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase().also { INSTANCE = it }
            }
        }
    }
}
