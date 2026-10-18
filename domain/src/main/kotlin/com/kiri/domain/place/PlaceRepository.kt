package com.kiri.domain.place

enum class PlaceCategory(val label: String) {
    CULTURE("전시·문화"), POPUP("팝업·행사"), SHOW("공연"), FOOD("맛집·카페"), OUTDOOR("산책·야외"), EXPERIENCE("체험·놀거리")
}
data class PlaceMission(val id: String, val title: String, val place: String, val region: String,
    val category: PlaceCategory, val rating: String? = null, val tags: List<String> = emptyList(),
    val pick: Boolean = false, val saved: Boolean = false,
    val searchMetadata: PlaceSearchMetadata? = null)
data class PlaceQuery(val keyword: String = "", val category: PlaceCategory? = null, val cursor: String? = null,
    val filters: PlaceFilters = PlaceFilters())
/** totalCount is supplied by the repository/server, not the loaded page size. */
data class PlacePage(val items: List<PlaceMission>, val totalCount: Int, val nextCursor: String? = null)
interface PlaceRepository {
    suspend fun load(query: PlaceQuery): PlacePage
    /** The authenticated user is resolved by the repository. Saves must be idempotent per user/mission. */
    suspend fun save(missionId: String)
    suspend fun saved(): List<PlaceMission>
}
