package mec0why.lime.data.repository

import android.util.LruCache
import mec0why.lime.data.api.SevenTVApi
import mec0why.lime.data.model.SevenTVEmote

class SevenTVRepository(
    private val api: SevenTVApi
) {
    private var globalEmotesMap: Map<String, SevenTVEmote>? = null
    private val channelEmotesCache = LruCache<Int, Map<String, SevenTVEmote>>(20)

    suspend fun getGlobalEmotes(): Result<Map<String, SevenTVEmote>> = runCatching {
        globalEmotesMap?.let { return@runCatching it }
        val response = api.getGlobalEmotes()
        val mapped = response.emotes.associateBy { it.name }
        globalEmotesMap = mapped
        mapped
    }

    suspend fun getChannelEmotes(kickUserId: Int): Result<Map<String, SevenTVEmote>> = runCatching {
        channelEmotesCache.get(kickUserId)?.let { return@runCatching it }
        val response = api.getChannelEmotes(kickUserId)
        val mapped = response.emote_set?.emotes?.associateBy { it.name } ?: emptyMap()
        channelEmotesCache.put(kickUserId, mapped)
        mapped
    }
}
