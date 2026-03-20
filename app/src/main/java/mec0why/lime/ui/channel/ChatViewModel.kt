package mec0why.lime.ui.channel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import mec0why.lime.LimeApp
import mec0why.lime.data.model.ChatMessageEvent
import mec0why.lime.data.model.PusherEvent
import mec0why.lime.data.model.SevenTVEmote
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val sevenTVRepository = (application as LimeApp).sevenTVRepository

    private val _messages = MutableStateFlow<List<ChatMessageEvent>>(emptyList())
    val messages: StateFlow<List<ChatMessageEvent>> = _messages

    private val _sevenTvEmotes = MutableStateFlow<Map<String, SevenTVEmote>>(emptyMap())
    val sevenTvEmotes: StateFlow<Map<String, SevenTVEmote>> = _sevenTvEmotes

    private val _userColors = MutableStateFlow<Map<String, String>>(emptyMap())
    val userColors: StateFlow<Map<String, String>> = _userColors


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

    fun connect(chatroomId: Int, kickUserId: Int?) {
        if (currentChatroomId == chatroomId) return
        disconnect()
        currentChatroomId = chatroomId
        _messages.value = emptyList()

        viewModelScope.launch {
            val globalEmotes = sevenTVRepository.getGlobalEmotes().getOrDefault(emptyMap())
            val channelEmotes = if (kickUserId != null) {
                sevenTVRepository.getChannelEmotes(kickUserId).getOrDefault(emptyMap())
            } else {
                emptyMap()
            }
            _sevenTvEmotes.value = globalEmotes + channelEmotes
        }

        val request = Request.Builder()
            .url("wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=8.4.0-rc2&flash=false")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                subscribeToChatroom(webSocket, chatroomId)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val pusherEvent = json.decodeFromString<PusherEvent>(text)
                    if (pusherEvent.event == "App\\Events\\ChatMessageEvent" && pusherEvent.data != null) {
                        val dataString = try {
                            pusherEvent.data.jsonPrimitive.contentOrNull ?: pusherEvent.data.toString()
                        } catch (e: Exception) {
                            pusherEvent.data.toString()
                        }
                        
                        val messageEvent = json.decodeFromString<ChatMessageEvent>(dataString)
                        addMessage(messageEvent)
                    }
                } catch (e: Exception) {
                    // Ignore parse errors silently
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Ignore WebSocket failures silently
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
        webSocket.send(jsonPayload)
    }

    private fun addMessage(message: ChatMessageEvent) {
        viewModelScope.launch {
            message.sender?.let { sender ->
                val color = sender.identity?.color
                if (!color.isNullOrBlank()) {
                    val currentColors = _userColors.value.toMutableMap()
                    currentColors[sender.username.lowercase()] = color
                    _userColors.value = currentColors
                }
            }
            val currentList = _messages.value.toMutableList()
            currentList.add(0, message)
            if (currentList.size > 100) {
                currentList.removeAt(currentList.lastIndex)
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
