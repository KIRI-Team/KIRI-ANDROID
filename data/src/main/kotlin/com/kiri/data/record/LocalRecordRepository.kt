package com.kiri.data.record

import com.kiri.data.local.*
import com.kiri.domain.detail.*
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.*
import com.kiri.domain.time.kiriToday
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Durable local activity records, isolated from explicit demo fixtures. */
open class LocalRecordRepository(
    private val home: HomeRepository,
    private val missions: MissionRepository,
    private val scheduledDetails: DetailRepository =
        com.kiri.data.detail.LocalDetailRepository(home, missions),
    private val familySchedules: com.kiri.domain.schedule.ScheduleRepository? = null,
    private val familyBuckets: BucketRepository? = null,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
    private val store: LocalStateStore = MemoryLocalStateStore(),
    private val media: RecordMediaStore = PassthroughRecordMediaStore,
    private val seedDemo: Boolean = false,
) : RecordRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow(RecordCollection())
    override val records = state.asStateFlow()
    private var scope: String? = null
    private var initialized = false
    private var syncedScheduleIds: Set<String> = emptySet()

    private suspend fun family(): com.kiri.domain.home.FamilyHome {
        try {
            val family = home.load()
            val next = family.localStorageScope()
            if (next != scope) {
                scope = null
                state.value = RecordCollection()
                initialized = false
                syncedScheduleIds = emptySet()
                val restored = next?.let { store.read("records/$it") }?.let(ActivityCodec::records)
                if (restored != null) {
                    state.value = restored
                    initialized = true
                    syncedScheduleIds =
                        restored.entries
                            .filter { it.kind == CalendarEntryKind.SCHEDULE }
                            .map { it.id }
                            .toSet()
                }
                scope = next
            }
            return family
        } catch (error: Exception) {
            scope = null
            initialized = false
            syncedScheduleIds = emptySet()
            state.value = RecordCollection()
            throw error
        }
    }

    private suspend fun publish(value: RecordCollection) {
        store.write("records/${requireNotNull(scope)}", ActivityCodec.records(value))
        state.value = value
        // A saved record is authoritative; photo cache cleanup must not turn a durable save into an
        // error.
        try {
            media.prune(
                requireNotNull(scope),
                value.photos.values.flatten() + value.memories.flatMap { it.photos },
            )
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: java.io.IOException) {
            // Retry cleanup with the next successful snapshot.
        }
    }

    override suspend fun refresh() = mutex.withLock {
        val family = family()
        if (family.browsing || scope == null) {
            state.value = RecordCollection()
            initialized = false
            syncedScheduleIds = emptySet()
            return@withLock
        }
        val members = family.members.filter { it.accessible }
        val participants = members.take(3).map { it.id }
        val today = clock.kiriToday()
        val question =
            family.question?.let {
                RecordedQuestion(
                    it.date,
                    it.text,
                    family.answers,
                    family.members.associate { member ->
                        member.id to if (member.accessible) member.nickname else "탈퇴한 멤버"
                    },
                    members.isNotEmpty() && members.all { member -> member.id in family.answers },
                )
            }
        val questions =
            (state.value.questions + listOfNotNull(question).associateBy { it.date }).mapValues {
                (_, past) ->
                past.copy(
                    memberNames =
                        past.memberNames.mapValues { (id, _) ->
                            family.members.find { it.id == id && it.accessible }?.nickname
                                ?: "탈퇴한 멤버"
                        }
                )
            }
        familySchedules?.refresh()
        familyBuckets?.refresh()
        val completedIds =
            state.value.entries
                .filter { it.kind == CalendarEntryKind.MEMORY && it.completed }
                .map { it.id }
                .toSet()
        familySchedules
            ?.schedules
            ?.value
            .orEmpty()
            .filter {
                it.id in completedIds &&
                    !it.completed &&
                    it.canReview(family.currentUserId, clock.instant())
            }
            .forEach { requireNotNull(familySchedules).complete(it.id) }
        val schedules =
            familySchedules
                ?.schedules
                ?.value
                ?.filter { !it.cancelled && !it.completed && it.id !in completedIds }
                ?.map { schedule ->
                    val time = schedule.startsAt.atZone(ZoneId.of("Asia/Seoul"))
                    CalendarEntry(
                        schedule.id,
                        schedule.missionId,
                        time.toLocalDate(),
                        CalendarEntryKind.SCHEDULE,
                        schedule.title,
                        time.toLocalTime(),
                        participantIds = schedule.acceptedIds.toList(),
                        status = schedule.status(clock.instant()),
                    )
                }
                ?: family.schedules
                    .filter { it.status == ScheduleStatus.CONFIRMED && it.finalParticipant }
                    .map { schedule ->
                        val time = schedule.startsAt.atZone(ZoneId.of("Asia/Seoul"))
                        CalendarEntry(
                            schedule.id,
                            schedule.missionId,
                            time.toLocalDate(),
                            CalendarEntryKind.SCHEDULE,
                            missions.load(schedule.missionId).summary.title,
                            time.toLocalTime(),
                            participantIds = participants,
                        )
                    }
        if (initialized) {
            val previous = state.value
            val refreshed = schedules.map { entry ->
                entry.copy(
                    conditions = previous.entries.find { it.id == entry.id }?.conditions.orEmpty()
                )
            }
            publish(
                previous.copy(
                    entries =
                        previous.entries.filterNot {
                            it.id in syncedScheduleIds && it.kind == CalendarEntryKind.SCHEDULE ||
                                familyBuckets != null && it.kind == CalendarEntryKind.BUCKET
                        } + refreshed + bucketEntries(),
                    familyName = family.familyName.orEmpty(),
                    currentUserId = family.currentUserId,
                    savers =
                        family.members.associate {
                            it.id to if (it.accessible) it.nickname else "탈퇴한 멤버"
                        },
                    questions = questions,
                    memories =
                        previous.memories.map { memory ->
                            if (family.members.any { it.id == memory.authorId && it.accessible })
                                memory
                            else memory.copy(authorName = "탈퇴한 멤버", role = "기타")
                        },
                )
            )
            syncedScheduleIds = schedules.map { it.id }.toSet()
            return@withLock
        }
        if (!seedDemo || family.serverBacked) {
            publish(
                RecordCollection(
                    entries = schedules + bucketEntries(),
                    familyName = family.familyName.orEmpty(),
                    currentUserId = family.currentUserId,
                    savers = family.members.associate { it.id to it.nickname },
                    questions = questions,
                )
            )
            initialized = true
            syncedScheduleIds = schedules.map { it.id }.toSet()
            return@withLock
        }
        val river =
            missions.load(com.kiri.data.mission.FakeMissionRepository.RIVER_MISSION_ID).summary
        val entries =
            schedules +
                listOf(
                    CalendarEntry(
                        "record-memory-1",
                        river.id,
                        today.minusDays(2),
                        CalendarEntryKind.MEMORY,
                        river.title,
                        participantIds = participants,
                        representativePhotoId = "group-1",
                    ),
                    CalendarEntry(
                        "record-memory-2",
                        river.id,
                        today.minusDays(4),
                        CalendarEntryKind.MEMORY,
                        river.title,
                        participantIds = participants,
                        representativePhotoId = "pixel-2",
                    ),
                ) +
                (if (familySchedules == null)
                    listOf(
                        CalendarEntry(
                            "record-upcoming",
                            river.id,
                            today.plusDays(3),
                            CalendarEntryKind.SCHEDULE,
                            river.title,
                            LocalTime.of(14, 50),
                            participantIds = participants,
                        )
                    )
                else emptyList()) +
                (if (familyBuckets == null)
                    listOf(
                        CalendarEntry(
                            "record-bucket",
                            river.id,
                            today.plusDays(1),
                            CalendarEntryKind.BUCKET,
                            river.title,
                            LocalTime.of(14, 50),
                            "father",
                            participants,
                        )
                    )
                else bucketEntries())
        val photos =
            entries
                .filter { it.kind == CalendarEntryKind.MEMORY }
                .associate { entry ->
                    val suffix = if (entry.id.endsWith("1")) "1" else "2"
                    entry.id to
                        (listOf(RecordPhoto("group-$suffix", "demo:group")) +
                            (2..3).map { RecordPhoto("group-$suffix-$it", "demo:group") } +
                            RecordPhoto("pixel-$suffix", "demo:pixel") +
                            (4..7).map { RecordPhoto("group-$suffix-$it", "demo:group") })
                }
        publish(
            RecordCollection(
                entries,
                entries
                    .filter { it.kind == CalendarEntryKind.MEMORY }
                    .map { entry ->
                        val author = members.first { it.id == family.currentUserId }
                        FamilyMemory(
                            "memory-${entry.id}",
                            entry.id,
                            author.id,
                            author.nickname,
                            author.role,
                            "아빠가 자주 가는 식당에 처음 가봤다. 좋아하는 메뉴를 같이 먹으니 아빠를 조금 더 알게 된 기분이었다.",
                            photos = photos[entry.id].orEmpty(),
                            rating = 4,
                            repeatActivity = true,
                        )
                    },
                photos,
                family.familyName.orEmpty(),
                family.currentUserId,
                family.members.associate { it.id to if (it.accessible) it.nickname else "탈퇴한 멤버" },
                questions,
            )
        )
        initialized = true
        syncedScheduleIds = schedules.map { it.id }.toSet()
    }

    private suspend fun bucketEntries(): List<CalendarEntry> =
        familyBuckets?.buckets?.value.orEmpty().map { bucket ->
            CalendarEntry(
                bucket.id,
                bucket.missionId,
                bucket.savedAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDate(),
                CalendarEntryKind.BUCKET,
                missions.load(bucket.missionId).summary.title,
                saverId = bucket.ownerId,
                completed = bucket.completed,
                linkedScheduleIds = bucket.linkedScheduleIds,
            )
        }

    override suspend fun detail(entryId: String): MissionDetail {
        refresh()
        return mutex.withLock {
            val family = family()
            check(!family.browsing) { "Family access required" }
            val entry =
                requireNotNull(state.value.entries.find { it.id == entryId }) { "Record not found" }
            val sharedSchedule = familySchedules?.schedules?.value?.find { it.id == entryId }
            if (
                entry.kind == CalendarEntryKind.SCHEDULE &&
                    sharedSchedule == null &&
                    family.schedules.any { it.id == entryId }
            )
                return@withLock scheduledDetails.load(entryId)
            val content = entry.missionSnapshot ?: missions.load(entry.missionId)
            MissionDetail(
                content.summary.id,
                content.summary.title,
                family.familyName.orEmpty(),
                entry.date,
                entry.startTime?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)),
                entry.placeSnapshot ?: content.summary.place,
                content.expectedSteps,
                content.durationMinutes,
                entry.participantIds.mapNotNull { id ->
                    family.members
                        .find { it.id == id }
                        ?.let {
                            DetailParticipant(if (it.accessible) it.nickname else "탈퇴한 멤버", true)
                        }
                },
                entry.kind == CalendarEntryKind.MEMORY,
                content.activities,
                content.essentials,
                content.nearby,
                entry.conditions,
            )
        }
    }

    override suspend fun applyConditions(
        entryId: String,
        conditions: Set<Condition>,
    ): MissionDetail {
        mutex.withLock {
            val family = family()
            check(!family.browsing)
            val target = requireNotNull(state.value.entries.find { it.id == entryId })
            check(
                target.status == ScheduleStatus.CONFIRMED &&
                    family.currentUserId in target.participantIds
            ) {
                "확정된 약속의 참여자만 조건을 변경할 수 있어요."
            }
            if (familySchedules == null && family.schedules.any { it.id == entryId }) {
                require(
                    state.value.entries.any {
                        it.id == entryId &&
                            it.kind == CalendarEntryKind.SCHEDULE &&
                            it.date == clock.kiriToday()
                    }
                )
                scheduledDetails.applyConditions(entryId, conditions)
            }
            val entry = requireNotNull(state.value.entries.find { it.id == entryId })
            check(entry.kind == CalendarEntryKind.SCHEDULE && entry.date == clock.kiriToday()) {
                "Only today's schedule can be customized"
            }
            publish(
                state.value.copy(
                    entries =
                        state.value.entries.map {
                            if (it.id == entryId) it.copy(conditions = conditions.toSet()) else it
                        }
                )
            )
        }
        return detail(entryId)
    }

    override suspend fun changeRepresentativePhoto(entryId: String, photoId: String) =
        mutex.withLock {
            check(!family().browsing)
            require(
                state.value.entries.any { it.id == entryId && it.kind == CalendarEntryKind.MEMORY }
            )
            require(state.value.photos[entryId].orEmpty().any { it.id == photoId }) {
                "Photo does not belong to this record"
            }
            publish(
                state.value.copy(
                    entries =
                        state.value.entries.map {
                            if (it.id == entryId) it.copy(representativePhotoId = photoId) else it
                        }
                )
            )
        }

    override suspend fun updateMemory(memory: FamilyMemory) = mutex.withLock {
        val family = family()
        check(!family.browsing)
        val original = requireNotNull(state.value.memories.find { it.id == memory.id })
        check(original.authorId == family.currentUserId)
        require(memory.authorId == original.authorId && memory.entryId == original.entryId)
        require(memory.isValidReview()) { "사진·기록·별점·재참여 의향은 필수예요." }
        val retained = media.retain(requireNotNull(scope), memory.photos.distinctBy { it.id })
        val updated =
            original.copy(
                text = memory.text.trim(),
                photos = retained,
                rating = memory.rating,
                repeatActivity = memory.repeatActivity,
            )
        publishMemories(
            state.value.memories.map { if (it.id == memory.id) updated else it },
            memory.entryId,
        )
    }

    override suspend fun deleteMemory(memoryId: String) = mutex.withLock {
        val family = family()
        check(!family.browsing)
        val memory = requireNotNull(state.value.memories.find { it.id == memoryId })
        check(memory.authorId == family.currentUserId)
        publishMemories(state.value.memories.filterNot { it.id == memoryId }, memory.entryId)
    }

    private suspend fun publishMemories(
        memories: List<FamilyMemory>,
        entryId: String,
        entries: List<CalendarEntry> = state.value.entries,
    ) {
        val photos =
            memories.filter { it.entryId == entryId }.flatMap { it.photos }.distinctBy { it.id }
        publish(
            state.value.copy(
                memories = memories,
                photos = state.value.photos + (entryId to photos),
                entries =
                    entries.map { entry ->
                        if (entry.id != entryId) entry
                        else
                            entry.copy(
                                representativePhotoId =
                                    entry.representativePhotoId?.takeIf { id ->
                                        photos.any { it.id == id }
                                    } ?: photos.firstOrNull()?.id
                            )
                    },
            )
        )
    }

    override suspend fun addMemory(
        entryId: String,
        text: String,
        photos: List<RecordPhoto>,
        rating: Int,
        repeatActivity: Boolean,
    ) = mutex.withLock {
        val family = family()
        check(!family.browsing)
        val author = family.members.first { it.id == family.currentUserId && it.accessible }
        val entry = requireNotNull(state.value.entries.find { it.id == entryId })
        var memory =
            FamilyMemory(
                java.util.UUID.randomUUID().toString(),
                entryId,
                author.id,
                author.nickname,
                author.role,
                text.trim(),
                photos.distinctBy { it.id },
                rating,
                repeatActivity,
            )
        require(memory.isValidReview()) { "사진·기록·별점·재참여 의향은 필수예요." }
        check(state.value.memories.none { it.entryId == entryId && it.authorId == author.id }) {
            "이미 기록을 남겼어요. 수정하기를 이용해주세요."
        }
        val schedule = familySchedules?.schedules?.value?.find { it.id == entryId }
        check(schedule != null && schedule.canReview(author.id, clock.instant())) {
            "활동 후 참여자만 기록할 수 있어요."
        }
        val snapshot = entry.missionSnapshot ?: missions.load(entry.missionId)
        memory = memory.copy(photos = media.retain(requireNotNull(scope), memory.photos))
        val previous = state.value
        publishMemories(
            previous.memories + memory,
            entry.id,
            previous.entries.map {
                if (it.id == entryId)
                    it.copy(
                        kind = CalendarEntryKind.MEMORY,
                        completed = true,
                        missionSnapshot = snapshot,
                        placeSnapshot = schedule.place,
                    )
                else it
            },
        )
        // The review is durable before completing the schedule. Refresh can repair an interrupted
        // completion.
        requireNotNull(familySchedules).complete(entryId)
    }

    override suspend fun deleteBucket(bucketId: String) {
        requireNotNull(familyBuckets).delete(bucketId)
        refresh()
    }

    override suspend fun requestSchedule(request: ScheduleRequest): CalendarEntry {
        if (familySchedules != null) {
            val (bucket, family) =
                mutex.withLock {
                    val family = family()
                    check(!family.browsing)
                    val bucket =
                        state.value.entries.find {
                            it.id == request.bucketId && it.kind == CalendarEntryKind.BUCKET
                        }
                    check(bucket != null || request.weeklyMissionId != null)
                    bucket to family
                }
            require(request.message.length <= 100)
            val start =
                java.time.LocalDateTime.of(request.date, request.time)
                    .atZone(java.time.ZoneId.of("Asia/Seoul"))
                    .toInstant()
            val schedule =
                familySchedules.create(
                    com.kiri.domain.schedule.ScheduleDraft(
                        bucket?.id.orEmpty(),
                        bucket?.missionId ?: requireNotNull(request.missionId),
                        start,
                        request.participantIds - family.currentUserId,
                        request.message,
                        missions.load(bucket?.missionId ?: requireNotNull(request.missionId)).summary.place,
                        request.weeklyMissionId,
                    ),
                    request.requestId.ifBlank {
                        "record-${bucket?.id ?: request.weeklyMissionId}-${request.date}-${request.time}"
                    },
                )
            refresh()
            return requireNotNull(state.value.entries.find { it.id == schedule.id })
        }
        return mutex.withLock {
            check(request.weeklyMissionId == null) { "주간 미션은 통합 일정 저장소가 필요해요." }
            val family = family()
            check(!family.browsing) { "Family access required" }
            val bucket =
                requireNotNull(
                    state.value.entries.find {
                        it.id == request.bucketId && it.kind == CalendarEntryKind.BUCKET &&
                            !it.completed && it.linkedScheduleIds.isEmpty()
                    }
                ) {
                    "Bucket not found"
                }
            require(!request.date.isBefore(clock.kiriToday())) { "Past dates cannot be requested" }
            val accessibleIds = family.members.filter { it.accessible }.map { it.id }.toSet()
            require(
                request.participantIds.isNotEmpty() &&
                    request.participantIds.all { it in accessibleIds }
            ) {
                "Participants must be family members"
            }
            require(request.message.length <= 100)
            val schedule =
                CalendarEntry(
                    "request-${bucket.id}",
                    bucket.missionId,
                    request.date,
                    CalendarEntryKind.SCHEDULE,
                    bucket.title,
                    request.time,
                    bucket.saverId,
                    request.participantIds.toList(),
                    status = ScheduleStatus.PENDING_RESPONSE,
                )
            publish(
                state.value.copy(
                    entries = state.value.entries.filterNot { it.id == bucket.id } + schedule
                )
            )
            schedule
        }
    }
}
