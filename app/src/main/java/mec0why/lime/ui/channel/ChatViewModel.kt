package mec0why.lime.ui.channel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import mec0why.lime.data.model.ChatMessageEvent
import mec0why.lime.data.model.PusherEvent
import mec0why.lime.data.model.PusherSubscribeData
import mec0why.lime.data.model.PusherSubscribeEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessageEvent>>(emptyList())
    val messages: StateFlow<List<ChatMessageEvent>> = _messages

    private var webSocket: WebSocket? = null
    private var currentChatroomId: Int? = null

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun connect(chatroomId: Int) {
        if (currentChatroomId == chatroomId) return
        Log.d("ChatVM", "Connecting to chatroom: $chatroomId")
        disconnect()
        currentChatroomId = chatroomId
        _messages.value = emptyList()

        val request = Request.Builder()
            .url("wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=8.4.0-rc2&flash=false")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatVM", "WebSocket Opened")
                subscribeToChatroom(webSocket, chatroomId)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ChatVM", "Message: $text")
                try {
                    val pusherEvent = json.decodeFromString<PusherEvent>(text)
                    if (pusherEvent.event == "App\\Events\\ChatMessageEvent" && pusherEvent.data != null) {
                        // In valid ChatMessageEvents, data is a JSON string containing the message details
                        val dataString = try {
                            pusherEvent.data.jsonPrimitive.contentOrNull ?: pusherEvent.data.toString()
                        } catch (e: Exception) {
                            pusherEvent.data.toString()
                        }
                        
                        val messageEvent = json.decodeFromString<ChatMessageEvent>(dataString)
                        addMessage(messageEvent)
                    }
                } catch (e: Exception) {
                    Log.e("ChatVM", "Parse error", e)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatVM", "WebSocket Failure", t)
            }
        })
    }

    private fun subscribeToChatroom(webSocket: WebSocket, chatroomId: Int) {
        val jsonPayload = """
            {
                "event": "pusher:subscribe",
                "data": {
                    "auth": "",
                    "channel": "chatrooms.$chatroomId.v2"
                }
            }
        """.trimIndent()
        Log.d("ChatVM", "Sending subscribe: $jsonPayload")
        webSocket.send(jsonPayload)
    }

    private fun addMessage(message: ChatMessageEvent) {
        viewModelScope.launch {
            val currentList = _messages.value.toMutableList()
            currentList.add(0, message)
            if (currentList.size > 100) {
                currentList.removeLast()
            }
            _messages.value = currentList
        }
    }

    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
        currentChatroomId = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
