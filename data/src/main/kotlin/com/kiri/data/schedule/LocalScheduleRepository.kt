package com.kiri.data.schedule

import com.kiri.data.local.*
import com.kiri.domain.home.*
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.BucketRepository
import com.kiri.domain.record.isValidReview
import com.kiri.domain.schedule.*
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Local implementation only. No endpoint, Push or third-party sending is simulated. */
class LocalScheduleRepository(
    private val home: HomeRepository,
    private val missions: MissionRepository,
    private val buckets: BucketRepository,
    private val clock: Clock,
    private val store: LocalStateStore = MemoryLocalStateStore(),
) : ScheduleRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<FamilySchedule>>(emptyList())
    override val schedules = state.asStateFlow()
    private val events = MutableStateFlow<List<ScheduleNotice>>(emptyList())
    override val notices = events.asStateFlow()
    private val requests = mutableMapOf<String, Pair<ScheduleDraft, String>>()
    private var scope: String? = null

    override suspend fun refresh() = mutex.withLock {
        val family = family()
        if (family.browsing) return@withLock
        val additions =
            (if (family.serverBacked) emptyList() else family.schedules)
                .filter { old -> state.value.none { it.id == old.id } }
                .map { old ->
                    val members = family.members.filter { it.accessible }.take(3).map { it.id }
                    FamilySchedule(
                        old.id,
                        null,
                        old.missionId,
                        missions.load(old.missionId).summary.title,
                        family.currentUserId,
                        old.startsAt,
                        old.responseDeadline ?: old.startsAt,
                        members.associateWith {
                            if (
                                old.status == ScheduleStatus.CONFIRMED || it == family.currentUserId
                            )
                                MemberResponse.ACCEPTED
                            else MemberResponse.PENDING
                        },
                        missions.load(old.missionId).summary.place,
                        cancelled = old.status == ScheduleStatus.CANCELLED,
                        completed = old.status == ScheduleStatus.COMPLETED,
                        weeklyMissionId =
                            old.weeklyMissionId ?: family.mission?.takeIf {
                                old.id == "schedule-demo" && it.id == "weekly-demo"
                            }?.id,
                    )
                }
        if (additions.isNotEmpty()) publish(state.value + additions)
        buckets.refresh()
        // Recover a process interruption between linking a bucket and persisting its schedule.
        val knownIds = state.value.filterNot { it.cancelled }.map { it.id }.toSet()
        buckets.buckets.value.forEach { bucket ->
            (bucket.linkedScheduleIds - knownIds).forEach { buckets.unlink(bucket.id, it) }
            if (!bucket.completed && state.value.any { it.bucketId == bucket.id && it.completed }) {
                buckets.complete(bucket.id)
            }
        }
    }

    override suspend fun create(draft: ScheduleDraft, requestId: String): FamilySchedule =
        mutex.withLock {
            require(requestId.isNotBlank())
            val family = family()
            requests[requestId]?.let { (previous, id) ->
                require(previous == draft) { "같은 요청 번호의 내용이 달라요." }
                return@withLock state.value.first { it.id == id }
            }
            validate(draft, family)
            checkDayIsFree(draft)
            if (draft.weeklyMissionId != null) {
                return@withLock createWeekly(draft, requestId, family)
            }
            buckets.refresh()
            val bucket =
                requireNotNull(
                    buckets.buckets.value.find {
                        it.id == draft.bucketId && it.missionId == draft.missionId
                    }
                ) {
                    "버킷을 다시 확인해주세요."
                }
            check(bucket.canCreateSchedule && !hasCompletion(bucket.id) &&
                state.value.none { it.bucketId == bucket.id && !it.cancelled }) {
                "이미 진행 중이거나 완료한 활동이에요."
            }
            val id = UUID.randomUUID().toString()
            val schedule =
                FamilySchedule(
                    id,
                    bucket.id,
                    draft.missionId,
                    missions.load(draft.missionId).summary.title,
                    family.currentUserId,
                    requireNotNull(draft.startsAt),
                    responseDeadline(requireNotNull(draft.startsAt), clock.instant()),
                    draft.inviteeIds.associateWith { MemberResponse.PENDING } +
                        (family.currentUserId to MemberResponse.ACCEPTED),
                    draft.place.trim(),
                    draft.message.trim(),
                )
            buckets.link(bucket.id, id)
            try {
                publish(
                    state.value + schedule,
                    events.value + notice(schedule, draft.inviteeIds, "새 약속 초대장이 도착했어요."),
                    requests + (requestId to (draft to id)),
                )
            } catch (error: Exception) {
                try {
                    buckets.unlink(bucket.id, id)
                } catch (rollback: Exception) {
                    error.addSuppressed(rollback)
                }
                throw error
            }
            schedule
        }

    private suspend fun createWeekly(
        draft: ScheduleDraft,
        requestId: String,
        family: FamilyHome,
    ): FamilySchedule {
        val weekly = requireNotNull(family.mission?.takeIf {
            it.id == draft.weeklyMissionId && it.missionId == draft.missionId
        }) { "이번 주 미션이 변경되었어요. 홈에서 다시 확인해주세요." }
        check(draft.bucketId.isBlank() && !weekly.completed && !weekly.unavailable) {
            "현재 진행할 수 없는 주간 미션이에요."
        }
        check(state.value.none { it.weeklyMissionId == weekly.id && !it.cancelled }) {
            "이미 주간 미션 일정이 있어요."
        }
        val localBuckets = requireNotNull(buckets as? com.kiri.data.record.LocalBucketRepository)
        buckets.refresh()
        val existing = buckets.buckets.value.find {
            it.ownerId == family.currentUserId && it.missionId == draft.missionId && !it.completed
        }
        check(existing == null || !hasCompletion(existing.id) &&
            state.value.none { it.bucketId == existing.id && !it.cancelled }) {
            "이미 진행 중이거나 완료한 활동이에요."
        }
        val id = UUID.randomUUID().toString()
        var created: FamilySchedule? = null
        var next: ScheduleState? = null
        // Once the final send starts committing, cancellation cannot leave stale in-memory state.
        return withContext(NonCancellable) {
            localBuckets.commitWeeklySchedule(draft.missionId, id, requireNotNull(scope), weekly.id, store) { bucket ->
                val schedule = FamilySchedule(
                    id, bucket.id, draft.missionId, weekly.title, family.currentUserId,
                    requireNotNull(draft.startsAt), responseDeadline(requireNotNull(draft.startsAt), clock.instant()),
                    draft.inviteeIds.associateWith { MemberResponse.PENDING } +
                        (family.currentUserId to MemberResponse.ACCEPTED),
                    draft.place.trim(), draft.message.trim(), weeklyMissionId = weekly.id,
                )
                val snapshot = ScheduleState(state.value + schedule,
                    events.value + notice(schedule, draft.inviteeIds, "새 약속 초대장이 도착했어요."),
                    requests + (requestId to (draft to id)))
                created = schedule
                next = snapshot
                mapOf("schedules/${requireNotNull(scope)}" to ActivityCodec.schedules(snapshot))
            }
            val committed = requireNotNull(next)
            state.value = committed.schedules
            events.value = committed.notices
            requests.clear()
            requests.putAll(committed.requests)
            requireNotNull(created)
        }
    }

    override suspend fun update(
        id: String,
        draft: ScheduleDraft,
        expectedRevision: Int,
    ): FamilySchedule = mutex.withLock {
        val family = family()
        val old = current(id, expectedRevision)
        check(old.proposerId == family.currentUserId && !old.cancelled && !old.completed) {
            "제안자만 약속을 수정할 수 있어요."
        }
        require(old.bucketId.orEmpty() == draft.bucketId && old.missionId == draft.missionId)
        val changed =
            old.startsAt != draft.startsAt ||
                old.place != draft.place.trim() ||
                (old.responses.keys - old.proposerId) != draft.inviteeIds
        validate(draft, family, requireFuture = changed)
        if (old.startsAt != draft.startsAt) checkDayIsFree(draft, exceptId = old.id)
        if (changed)
            check(clock.instant().isBefore(old.startsAt)) { "이미 시작한 약속의 핵심 정보는 수정할 수 없어요." }
        val updated =
            old.copy(
                startsAt = requireNotNull(draft.startsAt),
                place = draft.place.trim(),
                message = draft.message.trim(),
                revision = old.revision + 1,
                responseDeadline =
                    if (changed) responseDeadline(requireNotNull(draft.startsAt), clock.instant())
                    else old.responseDeadline,
                responses =
                    if (!changed) old.responses
                    else
                        draft.inviteeIds.associateWith {
                            if (
                                old.responses[it] == MemberResponse.ACCEPTED ||
                                    old.responses[it] == MemberResponse.RECONFIRM_REQUIRED
                            )
                                MemberResponse.RECONFIRM_REQUIRED
                            else old.responses[it] ?: MemberResponse.PENDING
                        } + (old.proposerId to MemberResponse.ACCEPTED),
            )
        replace(
            updated,
            if (changed)
                events.value +
                    notice(
                        updated,
                        old.responses.keys + updated.responses.keys - old.proposerId,
                        "약속 정보가 변경되었어요. 참여 여부를 다시 확인해주세요.",
                    )
            else events.value,
        )
        updated
    }

    override suspend fun respond(
        id: String,
        response: MemberResponse,
        expectedRevision: Int,
    ): FamilySchedule = mutex.withLock {
        val family = family()
        check(!family.browsing)
        require(response == MemberResponse.ACCEPTED || response == MemberResponse.DECLINED)
        val old = current(id, expectedRevision)
        check(old.canRespond(family.currentUserId, clock.instant())) { "응답 대상이 아니거나 응답 시간이 지났어요." }
        old.copy(
                responses = old.responses + (family.currentUserId to response),
                revision = old.revision + 1,
            )
            .also { replace(it) }
    }

    override suspend fun cancel(id: String, expectedRevision: Int) = mutex.withLock {
        val family = family()
        check(!family.browsing)
        val old = current(id, expectedRevision)
        check(old.proposerId == family.currentUserId && !old.completed) { "완료한 약속은 취소할 수 없어요." }
        buckets.refresh()
        check(buckets.buckets.value.none { it.id == old.bucketId && it.completed } &&
            !hasCompletion(old.bucketId, old.id)) { "완료한 버킷의 약속은 취소할 수 없어요." }
        if (!old.cancelled) {
            val updated = old.copy(cancelled = true, revision = old.revision + 1)
            replace(
                updated,
                events.value + notice(updated, old.responses.keys - old.proposerId, "약속이 취소되었어요."),
            )
            old.bucketId?.let { buckets.unlink(it, id) }
        }
    }

    override suspend fun complete(id: String) = mutex.withLock {
        val family = family()
        check(!family.browsing)
        val old = requireNotNull(state.value.find { it.id == id })
        check(old.canReview(family.currentUserId, clock.instant())) { "활동 후 참여자만 기록할 수 있어요." }
        if (!old.completed) replace(old.copy(completed = true, revision = old.revision + 1))
        old.bucketId?.let { buckets.complete(it) }
        Unit
    }

    private fun current(id: String, revision: Int): FamilySchedule {
        val schedule = requireNotNull(state.value.find { it.id == id }) { "약속을 찾을 수 없어요." }
        check(schedule.revision == revision) { "약속이 변경되었어요. 새로 확인해주세요." }
        return schedule
    }

    /** A durable review may exist even if the process stopped before marking completion. */
    private suspend fun hasCompletion(bucketId: String?, scheduleId: String? = null): Boolean {
        val related = state.value.filter {
            it.id == scheduleId || bucketId != null && it.bucketId == bucketId
        }
        if (related.any { it.completed }) return true
        val ids = related.map { it.id }.toSet()
        if (ids.isEmpty()) return false
        val records = store.read("records/${requireNotNull(scope)}")?.let(ActivityCodec::records)
        return records?.memories?.any { it.entryId in ids && it.isValidReview() } == true
    }

    private suspend fun family(): FamilyHome {
        try {
            val family = home.family()
            val next = family.localStorageScope()
            if (scope != next) {
                scope = null
                state.value = emptyList()
                events.value = emptyList()
                requests.clear()
                val restored =
                    next?.let { store.read("schedules/$it") }?.let(ActivityCodec::schedules)
                        ?: ScheduleState()
                state.value = restored.schedules
                events.value = restored.notices
                requests.putAll(restored.requests)
                scope = next
            }
            check(family.browsing || scope != null) { "현재 가족 구성원만 사용할 수 있어요." }
            return family
        } catch (error: Exception) {
            scope = null
            state.value = emptyList()
            events.value = emptyList()
            requests.clear()
            throw error
        }
    }

    private suspend fun publish(
        value: List<FamilySchedule>,
        notices: List<ScheduleNotice> = events.value,
        pending: Map<String, Pair<ScheduleDraft, String>> = requests.toMap(),
    ) {
        store.write(
            "schedules/${requireNotNull(scope)}",
            ActivityCodec.schedules(ScheduleState(value, notices, pending)),
        )
        state.value = value
        events.value = notices
        requests.clear()
        requests.putAll(pending)
    }

    // 하루에는 일정을 하나만 정할 수 있다. 그날 다른 일정(대기·확정·완료)이 있으면 만들거나 옮길 수 없다
    private fun checkDayIsFree(draft: ScheduleDraft, exceptId: String? = null) {
        val date = requireNotNull(draft.startsAt).atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
        check(date !in state.value.occupiedDates(clock.instant(), exceptId)) { ONE_SCHEDULE_PER_DAY_MESSAGE }
    }

    private suspend fun validate(
        draft: ScheduleDraft,
        family: FamilyHome,
        requireFuture: Boolean = true,
    ) {
        check(!family.browsing) { "가족 연결 후 약속을 만들 수 있어요." }
        val startsAt = requireNotNull(draft.startsAt) { "날짜와 시간을 선택해주세요." }
        require(!requireFuture || startsAt.isAfter(clock.instant())) { "앞으로의 날짜와 시간을 선택해주세요." }
        require(
            draft.inviteeIds.isNotEmpty() &&
                family.currentUserId !in draft.inviteeIds &&
                draft.inviteeIds.all { id -> family.members.any { it.id == id && it.accessible } }
        ) {
            "초대할 가족을 선택해주세요."
        }
        require(draft.message.length <= 500 && draft.place.isNotBlank()) { "장소와 한 줄 메시지를 확인해주세요." }
        val mission = missions.load(draft.missionId)
        require(!requireFuture || mission.scheduleWindow?.contains(startsAt) != false) {
            "행사 운영기간과 운영시간 안에서 선택해주세요."
        }
    }

    private suspend fun replace(
        schedule: FamilySchedule,
        notices: List<ScheduleNotice> = events.value,
    ) {
        publish(state.value.map { if (it.id == schedule.id) schedule else it }, notices)
    }

    private fun notice(schedule: FamilySchedule, recipients: Set<String>, text: String) =
        ScheduleNotice(
            "${schedule.id}-${schedule.revision}",
            schedule.id,
            recipients,
            text,
            clock.instant(),
        )
}
