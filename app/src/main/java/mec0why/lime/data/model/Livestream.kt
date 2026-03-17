package mec0why.lime.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LivestreamResponse(
    val data: List<Livestream> = emptyList(),
    val message: String = ""
)

@Serializable
data class Livestream(
    @SerialName("broadcaster_user_id") val broadcasterUserId: Int = 0,
    val category: LivestreamCategory? = null,
    @SerialName("channel_id") val channelId: Int = 0,
    @SerialName("custom_tags") val customTags: List<String> = emptyList(),
    @SerialName("has_mature_content") val hasMatureContent: Boolean = false,
    val language: String = "",
    @SerialName("profile_picture") val profilePicture: String = "",
    val slug: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("stream_title") val streamTitle: String = "",
    val thumbnail: String = "",
    @SerialName("viewer_count") val viewerCount: Int = 0
)

@Serializable
data class LivestreamCategory(
    val id: Int = 0,
    val name: String = "",
    val thumbnail: String = ""
)
