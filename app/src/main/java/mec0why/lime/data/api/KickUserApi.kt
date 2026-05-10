package mec0why.lime.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface KickUserApi {

    @POST("public/v1/chat")
    suspend fun sendChatMessage(@Body request: SendMessageRequest): SendMessageResponse

    @GET("public/v1/users")
    suspend fun getMe(): KickUserResponse
}

@Serializable
data class KickUserResponse(
    val data: List<KickUserData>? = null
)

@Serializable
data class KickUserData(
    @SerialName("user_id") val userId: Int,
    val name: String,
    @SerialName("profile_picture") val profilePicture: String? = null
)

@Serializable
data class SendMessageRequest(
    @SerialName("broadcaster_user_id") val broadcasterUserId: Int,
    val content: String,
    val type: String
)

@Serializable
data class SendMessageResponse(
    val data: SendMessageData? = null,
    val message: String = ""
)

@Serializable
data class SendMessageData(
    @SerialName("is_sent") val isSent: Boolean = false,
    @SerialName("message_id") val messageId: String = ""
)
