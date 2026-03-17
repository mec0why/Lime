package mec0why.lime.data.repository

import mec0why.lime.data.api.KickApi
import mec0why.lime.data.api.KickUnofficialApi
import mec0why.lime.data.model.Category
import mec0why.lime.data.model.ChannelResponse
import mec0why.lime.data.model.Livestream
import mec0why.lime.data.model.SearchChannel

class KickRepository(
    private val api: KickApi,
    private val unofficialApi: KickUnofficialApi
) {

    suspend fun getLivestreams(page: Int = 1): Result<List<Livestream>> = runCatching {
        api.getLivestreams(page = page).data
    }

    suspend fun getCategories(): Result<List<Category>> = runCatching {
        api.getCategories().data
    }

    suspend fun getChannel(slug: String): Result<ChannelResponse> = runCatching {
        unofficialApi.getChannel(slug)
    }

    suspend fun searchChannels(query: String): Result<List<SearchChannel>> = runCatching {
        unofficialApi.searchChannels(query).channels
    }
}
