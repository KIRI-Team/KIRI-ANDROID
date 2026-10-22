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
    /** Only explicitly known event dates/hours constrain scheduling. Permanent venues leave this null. */
    val scheduleWindow: MissionScheduleWindow? = null,
)

data class MissionScheduleWindow(
    val firstDate: java.time.LocalDate,
    val lastDate: java.time.LocalDate,
    val opensAt: java.time.LocalTime? = null,
    val closesAt: java.time.LocalTime? = null,
) {
    init { require(!lastDate.isBefore(firstDate)) }
    fun contains(time: java.time.Instant): Boolean {
        val local = time.atZone(java.time.ZoneId.of("Asia/Seoul"))
        if (local.toLocalDate().isBefore(firstDate) || local.toLocalDate().isAfter(lastDate)) return false
        val start = opensAt; val end = closesAt
        if (start == null || end == null) return true
        return if (end.isBefore(start)) !local.toLocalTime().isBefore(start) || !local.toLocalTime().isAfter(end)
        else !local.toLocalTime().isBefore(start) && !local.toLocalTime().isAfter(end)
    }
}

data class MissionReview(val id: String, val author: String, val text: String, val likes: Int)

interface MissionRepository {
    suspend fun catalog(): List<PlaceMission>
    suspend fun load(missionId: String): MissionContent
}
