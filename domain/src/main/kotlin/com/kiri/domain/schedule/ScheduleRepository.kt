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

// 하루에는 일정을 하나만 정할 수 있다. 응답 대기·재확인·확정·완료 일정이 그날을 차지하고, 취소·미확정 일정은 비운다
fun FamilySchedule.occupiesDay(now: Instant): Boolean = status(now) in DayOccupyingStatuses

// 일정이 있는 날(한국 시간 기준). 일정을 바꿀 때는 바꾸는 일정 자신(exceptId)을 뺀다
fun List<FamilySchedule>.occupiedDates(now: Instant, exceptId: String? = null): Set<java.time.LocalDate> =
    filter { it.id != exceptId && it.occupiesDay(now) }
        .map { it.startsAt.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate() }
        .toSet()

val DayOccupyingStatuses = setOf(
    ScheduleStatus.PENDING_RESPONSE,
    ScheduleStatus.RECONFIRM_REQUIRED,
    ScheduleStatus.CONFIRMED,
    ScheduleStatus.COMPLETED,
)

const val ONE_SCHEDULE_PER_DAY_MESSAGE = "이미 약속이 있는 날이에요. 다른 날짜를 골라주세요."

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
