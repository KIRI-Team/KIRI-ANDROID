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
    // 장소(없으면 미션) 사진 주소. 없으면 카드의 기본 이미지를 쓴다
    val imageUrl: String? = null,
    // 1인 기준 요금(원). 무료면 0, 확인할 수 없으면 null. 표시용이며 앱의 정렬(rankNearby)에는 쓰지 않는다
    val priceWon: Int? = null,
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
    // 걸음수 대신 서버가 주는 예상 걷기량("적음·보통·많음"). 걸음수가 있으면 걸음수를 먼저 보여준다
    val walkingLevel: String? = null,
    // 활동시간이 범위로 오면 durationMinutes 가 최소, 이 값이 최대다
    val durationMaxMinutes: Int? = null,
) {
    val stepsLabel: String get() = expectedSteps?.let { "${it}보" } ?: walkingLevel ?: "정보 없음"
    val durationLabel: String get() {
        val min = durationMinutes ?: return durationMaxMinutes?.let { "최대 ${formatMinutes(it)}" } ?: "정보 없음"
        val max = durationMaxMinutes
        return if (max == null || max == min) formatMinutes(min) else "${formatMinutes(min)} ~ ${formatMinutes(max)}"
    }

    private fun formatMinutes(minutes: Int) = when {
        minutes < 60 -> "${minutes}분"
        minutes % 60 == 0 -> "${minutes / 60}시간"
        else -> "${minutes / 60}시간 ${minutes % 60}분"
    }
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
