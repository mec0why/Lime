package mec0why.lime.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

object VerifiedSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Verified", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val input = decoder as? JsonDecoder ?: return false
        return try {
            when (val element = input.decodeJsonElement()) {
                is JsonPrimitive -> {
                    if (element.isString) {
                        element.content.lowercase() == "true" || element.content == "1"
                    } else {
                        element.booleanOrNull ?: (element.content == "1")
                    }
                }
                is JsonObject -> true
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

object LiveStatusSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LiveStatus", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val input = decoder as? JsonDecoder ?: return false
        return try {
            when (val element = input.decodeJsonElement()) {
                is JsonPrimitive -> {
                    if (element.isString) {
                        element.content.lowercase() == "true" || element.content == "1"
                    } else {
                        element.booleanOrNull ?: (element.content == "1")
                    }
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

@Serializable
data class ChannelResponse(
    val id: Int = 0,
    val slug: String = "",
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("user") val user: ChannelUser? = null,
    @SerialName("playback_url") val playbackUrl: String? = null,
    @SerialName("livestream") val livestream: ChannelLivestream? = null,
    @Serializable(with = VerifiedSerializer::class) val verified: Boolean = false,
    @SerialName("recent_categories") val recentCategories: List<RecentCategory> = emptyList(),
    @SerialName("chatroom") val chatroom: Chatroom? = null,
    @SerialName("subscriber_badges") val subscriberBadges: List<SubscriberBadge> = emptyList()
)

@Serializable
data class SubscriberBadge(
    val id: Int = 0,
    @SerialName("channel_id") val channelId: Int = 0,
    val months: Int = 0,
    @SerialName("badge_image") val badgeImage: BadgeImage? = null
)

@Serializable
data class BadgeImage(
    @SerialName("src") val src: String = "",
    val srcset: String = ""
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
    @SerialName("is_mature") val isMature: Boolean = false,
    @Serializable(with = LiveStatusSerializer::class) @SerialName("is_live") val isLive: Boolean = false,
    val thumbnail: ChannelThumbnail? = null
)

@Serializable
data class ChannelThumbnail(
    val url: String? = null,
    val src: String? = null
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
    @SerialName("livestream") val livestream: ChannelLivestream? = null,
    @Serializable(with = LiveStatusSerializer::class) @SerialName("isLive") val isLive: Boolean = false,
    @Serializable(with = LiveStatusSerializer::class) @SerialName("is_live_now") val isLiveNow: Boolean = false,
    @SerialName("live_stream") val liveStream: ChannelLivestream? = null,
    @SerialName("indexing") val indexing: Boolean = false,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @Serializable(with = VerifiedSerializer::class) val verified: Boolean = false
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
    val profilepic: String? = null,
    @Serializable(with = LiveStatusSerializer::class) @SerialName("is_live") val isLive: Boolean = false,
    @Serializable(with = VerifiedSerializer::class) @SerialName("verified") val verified: Boolean = false
) {
    val avatarUrl: String? get() = profilePic ?: profilePicOld ?: profilePicture ?: profileImage ?: profilepic
}
