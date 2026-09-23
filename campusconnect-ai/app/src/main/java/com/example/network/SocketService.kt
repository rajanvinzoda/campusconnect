package com.example.network

import com.example.data.model.ChatMessage
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class SocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTHENTICATED,
    RECONNECTING,
    ERROR
}

data class SocketAuthPayload(
    val token: String,
    val userId: String,
    val userName: String,
    val role: String
)

sealed class SocketEvent {
    data class NewMessage(val message: ChatMessage) : SocketEvent()
    data class NewNotification(val notification: NotificationItem) : SocketEvent()
    data class NewPost(val post: Post) : SocketEvent()
    data class RawEvent(val name: String, val payload: JSONObject) : SocketEvent()
}

class SocketService private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    private var activeAuth: SocketAuthPayload? = null

    fun connect(url: String = "wss://api.campusconnect.edu/v1/socket", auth: SocketAuthPayload? = null) {
        activeAuth = auth
        _connectionState.value = SocketConnectionState.CONNECTING
        scope.launch {
            kotlinx.coroutines.delay(600)
            _connectionState.value = if (auth != null) {
                SocketConnectionState.AUTHENTICATED
            } else {
                SocketConnectionState.CONNECTED
            }
        }
    }

    fun disconnect() {
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }

    fun emit(eventName: String, data: JSONObject) {
        scope.launch {
            _events.emit(SocketEvent.RawEvent(eventName, data))
        }
    }

    companion object {
        @Volatile
        private var instance: SocketService? = null

        fun getInstance(): SocketService {
            return instance ?: synchronized(this) {
                instance ?: SocketService().also { instance = it }
            }
        }
    }
}
