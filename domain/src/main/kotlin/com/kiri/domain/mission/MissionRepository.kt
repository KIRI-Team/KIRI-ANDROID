package com.kiri.domain.mission

import com.kiri.domain.detail.DetailSuggestion
import com.kiri.domain.detail.NearbyPlace
import com.kiri.domain.place.PlaceMission

/** Mission content is shared by discovery and scheduled activities; schedule state is separate. */
data class MissionContent(
    val summary: PlaceMission,
    val expectedSteps: Int? = null,
    val durationMinutes: Int? = null,
    val activities: List<DetailSuggestion> = emptyList(),
    val essentials: List<DetailSuggestion> = emptyList(),
    val nearby: List<NearbyPlace> = emptyList(),
    val reviews: List<MissionReview> = emptyList(),
    val description: String = "",
)

data class MissionReview(val id: String, val author: String, val text: String, val likes: Int)

interface MissionRepository {
    suspend fun catalog(): List<PlaceMission>
    suspend fun load(missionId: String): MissionContent
}
