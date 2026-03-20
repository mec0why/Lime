package mec0why.lime.data.repository

import mec0why.lime.data.api.SevenTVApi
import mec0why.lime.data.model.SevenTVEmote

class SevenTVRepository(
    private val api: SevenTVApi
) {
    private var globalEmotesMap: Map<String, SevenTVEmote>? = null

    suspend fun getGlobalEmotes(): Result<Map<String, SevenTVEmote>> = runCatching {
        globalEmotesMap?.let { return@runCatching it }
        val response = api.getGlobalEmotes()
        val mapped = response.emotes.associateBy { it.name }
        globalEmotesMap = mapped
        mapped
    }

    suspend fun getChannelEmotes(kickUserId: Int): Result<Map<String, SevenTVEmote>> = runCatching {
        val response = api.getChannelEmotes(kickUserId)
        response.emote_set?.emotes?.associateBy { it.name } ?: emptyMap()
    }
}
