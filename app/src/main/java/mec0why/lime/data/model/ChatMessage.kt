package mec0why.lime.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessageEvent(
    val id: String = "",
    @SerialName("chatroom_id") val chatroomId: Int = 0,
    val content: String = "",
    val type: String = "message",
    @SerialName("created_at") val createdAt: String = "",
    val sender: ChatSender? = null,
    val metadata: ChatMetadata? = null
)

@Serializable
data class ChatMetadata(
    @SerialName("original_message") val originalMessage: OriginalMessage? = null,
    @SerialName("original_sender") val originalSender: ChatSender? = null
)

@Serializable
data class OriginalMessage(
    val id: String = "",
    val content: String = ""
)

@Serializable
data class ChatSender(
    val id: Int = 0,
    val username: String = "",
    val slug: String = "",
    val identity: ChatIdentity? = null
)

@Serializable
data class ChatIdentity(
    val color: String = "",
    val badges: List<ChatBadge> = emptyList()
)

@Serializable
data class ChatBadge(
    val type: String = "",
    val text: String = "",
    val count: Int = 0
)

@Serializable
data class PusherEvent(
    val event: String,
    val data: JsonElement? = null,
    val channel: String? = null
)

@Serializable
data class PusherSubscribeData(
    val channel: String,
    val auth: String = ""
)

@Serializable
data class PusherSubscribeEvent(
    val event: String = "pusher:subscribe",
    val data: PusherSubscribeData
)
