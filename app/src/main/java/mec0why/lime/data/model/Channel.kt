package mec0why.lime.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelResponse(
    val id: Int = 0,
    val slug: String = "",
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("user") val user: ChannelUser? = null,
    @SerialName("playback_url") val playbackUrl: String? = null,
    @SerialName("livestream") val livestream: ChannelLivestream? = null,
    val verified: Boolean = false,
    @SerialName("recent_categories") val recentCategories: List<RecentCategory> = emptyList(),
    @SerialName("chatroom") val chatroom: Chatroom? = null
)

@Serializable
data class Chatroom(
    val id: Int = 0,
    @SerialName("chatable_type") val chatableType: String = "",
    @SerialName("channel_id") val channelId: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ChannelUser(
    val id: Int = 0,
    val username: String = "",
    val bio: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null
)

@Serializable
data class ChannelLivestream(
    val id: Int = 0,
    @SerialName("session_title") val sessionTitle: String = "",
    @SerialName("viewer_count") val viewerCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    val categories: List<RecentCategory> = emptyList(),
    @SerialName("is_mature") val isMature: Boolean = false
)

@Serializable
data class RecentCategory(
    val id: Int = 0,
    val name: String = "",
    val slug: String = "",
    val icon: String? = null
)

@Serializable
data class SearchChannel(
    val id: Int = 0,
    val slug: String = "",
    val user: SearchUser? = null,
)

@Serializable
data class SearchResponse(
    val channels: List<SearchChannel> = emptyList()
)

@Serializable
data class SearchUser(
    val id: Int = 0,
    val username: String = "",
    @SerialName("profilePic") val profilePic: String? = null,
    @SerialName("profile_pic") val profilePicOld: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    val profilepic: String? = null
) {
    val avatarUrl: String? get() = profilePic ?: profilePicOld ?: profilePicture ?: profileImage ?: profilepic
}
