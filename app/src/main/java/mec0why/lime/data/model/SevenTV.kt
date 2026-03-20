package mec0why.lime.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SevenTVGlobalEmoteSet(
    val emotes: List<SevenTVEmote> = emptyList()
)

@Serializable
data class SevenTVChannelResponse(
    val emote_set: SevenTVEmoteSet? = null
)

@Serializable
data class SevenTVEmoteSet(
    val emotes: List<SevenTVEmote> = emptyList()
)

@Serializable
data class SevenTVEmote(
    val id: String = "",
    val name: String = "",
    val flags: Int = 0,
    val data: SevenTVEmoteData? = null
)

@Serializable
data class SevenTVEmoteData(
    val id: String = "",
    val name: String = "",
    val flags: Int = 0,
    val host: SevenTVHost? = null
)

@Serializable
data class SevenTVHost(
    val url: String = "",
    val files: List<SevenTVFile> = emptyList()
)

@Serializable
data class SevenTVFile(
    val name: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val format: String = ""
)
