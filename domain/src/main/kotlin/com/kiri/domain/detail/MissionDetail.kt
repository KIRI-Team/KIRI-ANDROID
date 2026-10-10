package com.kiri.domain.detail

import java.time.LocalDate
import com.kiri.domain.mission.MissionReview

enum class Condition { LESS_WALKING, MORE_REST, LOWER_BUDGET, AVOID_CROWDS }
data class DetailParticipant(val name: String, val participating: Boolean)
data class DetailSuggestion(val title: String, val description: String)
data class NearbyPlace(
    val id: String,
    val title: String,
    val address: String,
    val description: String,
    val distanceMeters: Int? = null,
    val restAvailable: Boolean? = null,
    val costWon: Int? = null,
    /** Only populated from a reliable source, never inferred from the place name. */
    val crowdLevel: Int? = null,
)

data class MissionDetail(
    val id: String,
    val title: String,
    val familyName: String,
    val date: LocalDate?,
    val startTime: String?,
    val place: String?,
    val expectedSteps: Int?,
    val durationMinutes: Int?,
    val participants: List<DetailParticipant>,
    val completed: Boolean,
    val activities: List<DetailSuggestion>,
    val essentials: List<DetailSuggestion>,
    val nearby: List<NearbyPlace>,
    val conditions: Set<Condition> = emptySet(),
) {
    val stepsLabel: String get() = expectedSteps?.let { "${it}보" } ?: "정보 없음"
    val durationLabel: String get() = durationMinutes?.let {
        if (it % 60 == 0) "${it / 60}시간" else "${it / 60}시간 ${it % 60}분"
    } ?: "정보 없음"
}

/** Each known criterion contributes a normalized rank. Missing data is not invented.
 * With no usable criteria, the source order is preserved. The main mission is untouched.
 */
fun rankNearby(places: List<NearbyPlace>, conditions: Set<Condition>): List<NearbyPlace> {
    val scores = DoubleArray(places.size)
    conditions.forEach { condition ->
        val values = places.map { place -> when (condition) {
            Condition.LESS_WALKING -> place.distanceMeters
            Condition.MORE_REST -> place.restAvailable?.let { if (it) 0 else 1 }
            Condition.LOWER_BUDGET -> place.costWon
            Condition.AVOID_CROWDS -> place.crowdLevel
        } }
        val known = values.filterNotNull().distinct().sorted()
        if (known.isNotEmpty()) values.forEachIndexed { index, value ->
            scores[index] += if (value == null) 1.0 else known.indexOf(value).toDouble() / known.size
        }
    }
    return places.indices.sortedBy { scores[it] }.map(places::get)
}

interface DetailRepository {
    suspend fun load(missionId: String): MissionDetail
    suspend fun applyConditions(missionId: String, conditions: Set<Condition>): MissionDetail
    suspend fun preview(missionId: String): MissionPreview = MissionPreview(load(missionId))
}

data class MissionPreview(val detail: MissionDetail, val reviews: List<MissionReview> = emptyList(), val curated: Boolean = false)
