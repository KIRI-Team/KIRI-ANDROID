package com.kiri.domain.preferences

import com.kiri.domain.place.PlaceCategory

enum class Environment(val label: String) { INDOOR("실내"), OUTDOOR("야외"), ANY("상관없어요") }
enum class Intensity(val label: String) { LOW("가볍게"), MEDIUM("보통"), HIGH("걷기 많아도 괜찮음") }
enum class Budget(val label: String) { FREE("무료 위주"), LOW("부담 없이"), ANY("상관없어요") }
enum class ActivityDuration(val label: String) { SHORT("1시간 이내"), MEDIUM("1~3시간"), LONG("반나절 이상"), ANY("상관없음") }
data class UserPreferences(
    val categories: Set<PlaceCategory> = emptySet(),
    val environment: Environment? = Environment.ANY, val intensity: Intensity? = Intensity.MEDIUM,
    val budget: Budget? = Budget.ANY, val duration: ActivityDuration? = ActivityDuration.ANY,
) {
    val valid get() = categories.size <= 3 && environment != null && intensity != null && budget != null && duration != null
}
interface PreferenceRepository {
    suspend fun load(): UserPreferences
    suspend fun save(preferences: UserPreferences)
}
