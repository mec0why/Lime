package mec0why.lime.data.api

import mec0why.lime.data.model.ChannelResponse
import mec0why.lime.data.model.LivestreamResponse
import mec0why.lime.data.model.OfficialCategoriesResponse
import mec0why.lime.data.model.SearchChannel
import mec0why.lime.data.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KickApi {

    @GET("public/v1/livestreams")
    suspend fun getLivestreams(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 25,
        @Query("sort") sort: String = "viewer_count"
    ): LivestreamResponse

    @GET("public/v1/categories")
    suspend fun getCategories(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): OfficialCategoriesResponse

}

interface KickUnofficialApi {

    @GET("api/v2/channels/{slug}")
    suspend fun getChannel(
        @Path("slug") slug: String
    ): ChannelResponse

    @GET("api/search")
    suspend fun searchChannels(
        @Query("searched_word") query: String
    ): SearchResponse
}
