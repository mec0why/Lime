package mec0why.lime.data.api

import mec0why.lime.data.model.SevenTVChannelResponse
import mec0why.lime.data.model.SevenTVGlobalEmoteSet
import retrofit2.http.GET
import retrofit2.http.Path

interface SevenTVApi {

    @GET("emote-sets/global")
    suspend fun getGlobalEmotes(): SevenTVGlobalEmoteSet

    @GET("users/kick/{kickUserId}")
    suspend fun getChannelEmotes(
        @Path("kickUserId") kickUserId: Int
    ): SevenTVChannelResponse
}
