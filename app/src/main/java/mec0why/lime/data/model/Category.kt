package mec0why.lime.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OfficialCategoriesResponse(
    val data: List<Category> = emptyList(),
    val message: String = "",
    val pagination: Pagination? = null
)

@Serializable
data class Category(
    val id: Int = 0,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val thumbnail: String = "",
    @SerialName("viewer_count") val viewerCount: Int = 0
)

@Serializable
data class Pagination(
    @SerialName("next_cursor") val nextCursor: String? = null
)
