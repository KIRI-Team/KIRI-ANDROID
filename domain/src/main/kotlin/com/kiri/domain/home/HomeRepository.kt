package com.kiri.domain.home

import java.time.LocalDate

/** A daily assignment identifies both the family question and its KST date. */
data class DailyQuestion(val id: String, val date: LocalDate, val text: String)

data class FamilyMember(
    val id: String,
    val nickname: String,
    val role: String,
    val accessible: Boolean = true,
)

data class WeeklyMission(
    val id: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val completed: Boolean,
    val place: String? = null,
    val keywords: List<String> = emptyList(),
    val imageUrl: String? = null,
    val unavailable: Boolean = false,
    val missionId: String = id,
    val recommendationOnly: Boolean = false,
)

data class HomeRecommendation(
    val id: String,
    val title: String,
    val place: String,
    val keywords: List<String> = emptyList(),
    val rating: String? = null,
    val imageUrl: String? = null,
    val description: String = "",
)

// 일정에 초대할 수 있는 가족. 나와 탈퇴한 구성원은 뺀다
fun FamilyHome.invitableMembers(): List<FamilyMember> = members.filter { it.accessible && it.id != currentUserId }

data class FamilyHome(
    val date: LocalDate,
    val currentUserId: String,
    val familyName: String?,
    val members: List<FamilyMember>,
    val question: DailyQuestion?,
    val answers: Map<String, String>,
    val pokedMemberIds: Set<String>,
    val mission: WeeklyMission?,
    val schedules: List<HomeSchedule> = emptyList(),
    val recommendations: List<HomeRecommendation> = emptyList(),
    val inviteCode: String? = null,
    val serverBacked: Boolean = false,
    val familyId: String? = null,
) {
    val browsing: Boolean
        get() = familyName == null

    fun canRead(memberId: String): Boolean =
        !browsing && question != null && members.any { it.id == memberId && it.accessible }

    fun canPoke(memberId: String): Boolean =
        canRead(memberId) && memberId != currentUserId && memberId !in pokedMemberIds
}

enum class ScheduleStatus {
    PENDING_RESPONSE,
    RECONFIRM_REQUIRED,
    CONFIRMED,
    NOT_CONFIRMED,
    CANCELLED,
    COMPLETED,
}

data class HomeSchedule(
    val id: String,
    val title: String,
    val startsAt: java.time.Instant,
    val status: ScheduleStatus,
    val finalParticipant: Boolean,
    val responseDeadline: java.time.Instant? = null,
    val missionId: String = id,
    val weeklyMissionId: String? = null,
)

data class HomeScheduleCard(val schedule: HomeSchedule, val kind: Kind) {
    enum class Kind {
        RESPONSE_REQUIRED,
        TODAY,
        UPCOMING,
    }
}

fun selectHomeSchedule(
    schedules: List<HomeSchedule>,
    now: java.time.Instant,
    zone: java.time.ZoneId = java.time.ZoneId.of("Asia/Seoul"),
): HomeScheduleCard? {
    val current = java.time.ZonedDateTime.ofInstant(now, zone)
    val active = schedules.filter {
        it.status != ScheduleStatus.CANCELLED && it.status != ScheduleStatus.COMPLETED
    }
    val response =
        active
            .filter {
                it.status == ScheduleStatus.PENDING_RESPONSE ||
                    it.status == ScheduleStatus.RECONFIRM_REQUIRED
            }
            .filter { it.responseDeadline == null || !now.isAfter(it.responseDeadline) }
            .minByOrNull { it.startsAt }
    if (response != null) return HomeScheduleCard(response, HomeScheduleCard.Kind.RESPONSE_REQUIRED)
    val confirmed = active.filter { it.status == ScheduleStatus.CONFIRMED && it.finalParticipant }
    val today =
        confirmed
            .filter {
                java.time.ZonedDateTime.ofInstant(it.startsAt, zone).toLocalDate() ==
                    current.toLocalDate()
            }
            .minByOrNull { it.startsAt }
    if (today != null) return HomeScheduleCard(today, HomeScheduleCard.Kind.TODAY)
    val upcoming =
        confirmed
            .filter {
                val days =
                    java.time.temporal.ChronoUnit.DAYS.between(
                        current.toLocalDate(),
                        java.time.ZonedDateTime.ofInstant(it.startsAt, zone).toLocalDate(),
                    )
                days in 1..7
            }
            .minByOrNull { it.startsAt }
    return upcoming?.let { HomeScheduleCard(it, HomeScheduleCard.Kind.UPCOMING) }
}

enum class HomeFailure {
    ANSWER_TOO_LONG,
    EMPTY_ANSWER,
    UNAVAILABLE,
    EXPIRED,
    ALREADY_ANSWERED,
    ALREADY_POKED,
    NETWORK,
}

class HomeException(val reason: HomeFailure) : Exception(reason.name)

/**
 * Implementations must validate access, assignment date and poke uniqueness atomically. The
 * authenticated current user is the answer author; callers cannot select another author. A
 * successful mutation returns the complete consistent snapshot.
 */
interface HomeRepository {
    suspend fun load(): FamilyHome

    suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome

    suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome
}
