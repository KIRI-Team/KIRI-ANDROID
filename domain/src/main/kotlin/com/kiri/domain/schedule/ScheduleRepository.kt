package com.kiri.domain.schedule

import com.kiri.domain.home.ScheduleStatus
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant

enum class MemberResponse(val label: String) {
    PENDING("미응답"), ACCEPTED("수락"), DECLINED("거절"), RECONFIRM_REQUIRED("재확인 필요")
}

data class ScheduleDraft(
    val bucketId: String,
    val missionId: String,
    val startsAt: Instant? = null,
    val inviteeIds: Set<String> = emptySet(),
    val message: String = "",
    val place: String = "",
    /** Family assignment identity; absent for ordinary bucket schedules. */
    val weeklyMissionId: String? = null,
)

data class FamilySchedule(
    val id: String,
    val bucketId: String?,
    val missionId: String,
    val title: String,
    val proposerId: String,
    val startsAt: Instant,
    val responseDeadline: Instant,
    val responses: Map<String, MemberResponse>,
    val place: String,
    val message: String = "",
    val revision: Int = 1,
    val cancelled: Boolean = false,
    /** Completion is historical: removing the last review does not erase an activity. */
    val completed: Boolean = false,
    val weeklyMissionId: String? = null,
) {
    val acceptedIds get() = responses.filterValues { it == MemberResponse.ACCEPTED }.keys
    fun status(now: Instant): ScheduleStatus = when {
        completed -> ScheduleStatus.COMPLETED
        cancelled -> ScheduleStatus.CANCELLED
        acceptedIds.any { it != proposerId } -> ScheduleStatus.CONFIRMED
        !now.isBefore(responseDeadline) -> ScheduleStatus.NOT_CONFIRMED
        else -> ScheduleStatus.PENDING_RESPONSE
    }
    fun canRespond(userId: String, now: Instant) = !cancelled && !completed &&
        userId != proposerId && userId in responses && now.isBefore(responseDeadline)
    fun canReview(userId: String, now: Instant) = !cancelled && userId in acceptedIds &&
        !now.isBefore(startsAt) && status(now) in setOf(ScheduleStatus.CONFIRMED, ScheduleStatus.COMPLETED)
}

fun responseDeadline(startsAt: Instant, createdAt: Instant): Instant {
    require(startsAt.isAfter(createdAt)) { "지난 시각에는 약속을 만들 수 없어요." }
    val dayBefore = startsAt.minus(Duration.ofHours(24))
    return if (createdAt.isBefore(dayBefore)) dayBefore else startsAt
}

data class ScheduleNotice(val id: String, val scheduleId: String, val recipientIds: Set<String>, val message: String, val createdAt: Instant)

/** One shared source for family-wide state and each member's independent response. */
interface ScheduleRepository {
    val schedules: StateFlow<List<FamilySchedule>>
    val notices: StateFlow<List<ScheduleNotice>>
    suspend fun refresh()
    suspend fun create(draft: ScheduleDraft, requestId: String): FamilySchedule
    suspend fun update(id: String, draft: ScheduleDraft, expectedRevision: Int): FamilySchedule
    suspend fun respond(id: String, response: MemberResponse, expectedRevision: Int): FamilySchedule
    suspend fun cancel(id: String, expectedRevision: Int)
    suspend fun complete(id: String)
}
