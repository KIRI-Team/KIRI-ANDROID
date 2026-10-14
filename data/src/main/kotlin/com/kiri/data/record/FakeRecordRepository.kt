package com.kiri.data.record

import com.kiri.domain.detail.*
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Session-only demo adapter. The API adapter can replace this binding without changing UI. */
class FakeRecordRepository(
    private val home: HomeRepository,
    private val missions: MissionRepository,
    private val scheduledDetails: DetailRepository = com.kiri.data.detail.FakeDetailRepository(home, missions),
) : RecordRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow(RecordCollection())
    override val records = state.asStateFlow()
    private var initialized = false

    override suspend fun refresh() = mutex.withLock {
        val family = home.load()
        if (family.browsing) {
            state.value = RecordCollection()
            initialized = false
            return@withLock
        }
        if (initialized) return@withLock
        val river = missions.load(com.kiri.data.mission.FakeMissionRepository.RIVER_MISSION_ID).summary
        val members = family.members.filter { it.accessible }
        val participants = members.take(3).map { it.id }
        val today = family.date
        val schedules = family.schedules.filter { it.status == ScheduleStatus.CONFIRMED && it.finalParticipant }.map { schedule ->
            val time = schedule.startsAt.atZone(ZoneId.of("Asia/Seoul"))
            CalendarEntry(schedule.id, schedule.missionId, time.toLocalDate(), CalendarEntryKind.SCHEDULE,
                missions.load(schedule.missionId).summary.title, time.toLocalTime(), participantIds = participants)
        }
        val entries = schedules + listOf(
            CalendarEntry("record-upcoming", river.id, today.plusDays(3), CalendarEntryKind.SCHEDULE, river.title, LocalTime.of(14, 50), participantIds = participants),
            CalendarEntry("record-bucket", river.id, today.plusDays(1), CalendarEntryKind.BUCKET, river.title, LocalTime.of(14, 50), "father", participants),
            CalendarEntry("record-memory-1", river.id, today.minusDays(2), CalendarEntryKind.MEMORY, river.title, participantIds = participants, representativePhotoId = "group-1"),
            CalendarEntry("record-memory-2", river.id, today.minusDays(4), CalendarEntryKind.MEMORY, river.title, participantIds = participants, representativePhotoId = "pixel-2"),
        )
        val photos = entries.filter { it.kind == CalendarEntryKind.MEMORY }.associate { entry ->
            val suffix = if (entry.id.endsWith("1")) "1" else "2"
            entry.id to (listOf(RecordPhoto("group-$suffix", "demo:group")) +
                (2..3).map { RecordPhoto("group-$suffix-$it", "demo:group") } +
                RecordPhoto("pixel-$suffix", "demo:pixel") + (4..7).map { RecordPhoto("group-$suffix-$it", "demo:group") })
        }
        state.value = RecordCollection(entries, entries.filter { it.kind == CalendarEntryKind.MEMORY }.map { entry ->
            val author = members.first { it.id == family.currentUserId }
            FamilyMemory("memory-${entry.id}", entry.id, author.id, author.nickname, author.role,
                "아빠가 자주 가는 식당에 처음 가봤다. 좋아하는 메뉴를 같이 먹으니 아빠를 조금 더 알게 된 기분이었다.", rating = 4, repeatActivity = true)
        }, photos, family.familyName.orEmpty(), family.currentUserId, members.associate { it.id to it.role })
        initialized = true
    }

    override suspend fun detail(entryId: String): MissionDetail {
        refresh()
        val family = home.load()
        check(!family.browsing) { "Family access required" }
        val entry = requireNotNull(state.value.entries.find { it.id == entryId }) { "Record not found" }
        if (entry.kind == CalendarEntryKind.SCHEDULE && family.schedules.any { it.id == entryId }) return scheduledDetails.load(entryId)
        val content = missions.load(entry.missionId)
        return MissionDetail(content.summary.id, content.summary.title, family.familyName.orEmpty(), entry.date,
            entry.startTime?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)), content.summary.place,
            content.expectedSteps, content.durationMinutes,
            entry.participantIds.mapNotNull { id -> family.members.find { it.id == id && it.accessible }?.let { DetailParticipant(it.role, true) } },
            entry.kind == CalendarEntryKind.MEMORY, content.activities, content.essentials, content.nearby, entry.conditions)
    }

    override suspend fun applyConditions(entryId: String, conditions: Set<Condition>): MissionDetail {
        val family = home.load()
        check(!family.browsing)
        if (family.schedules.any { it.id == entryId }) {
            require(state.value.entries.any { it.id == entryId && it.kind == CalendarEntryKind.SCHEDULE && it.date == family.date })
            scheduledDetails.applyConditions(entryId, conditions)
        }
        mutex.withLock {
            val entry = requireNotNull(state.value.entries.find { it.id == entryId })
            check(entry.kind == CalendarEntryKind.SCHEDULE && entry.date == family.date) { "Only today's schedule can be customized" }
            state.value = state.value.copy(entries = state.value.entries.map { if (it.id == entryId) it.copy(conditions = conditions.toSet()) else it })
        }
        return detail(entryId)
    }

    override suspend fun changeRepresentativePhoto(entryId: String, photoId: String) = mutex.withLock {
        check(!home.load().browsing)
        require(state.value.entries.any { it.id == entryId && it.kind == CalendarEntryKind.MEMORY })
        require(state.value.photos[entryId].orEmpty().any { it.id == photoId }) { "Photo does not belong to this record" }
        state.value = state.value.copy(entries = state.value.entries.map { if (it.id == entryId) it.copy(representativePhotoId = photoId) else it })
    }

    override suspend fun updateMemory(memory: FamilyMemory) = mutex.withLock {
        val family = home.load()
        check(!family.browsing)
        val original = requireNotNull(state.value.memories.find { it.id == memory.id })
        check(original.authorId == family.currentUserId)
        require(memory.authorId == original.authorId && memory.entryId == original.entryId)
        require(memory.text.isNotBlank() && memory.text.length <= 500 && memory.rating in 1..5 && memory.repeatActivity != null)
        val updated = original.copy(text = memory.text.trim(), photos = memory.photos.distinctBy { it.id }, rating = memory.rating, repeatActivity = memory.repeatActivity)
        state.value = state.value.copy(memories = state.value.memories.map { if (it.id == memory.id) updated else it },
            photos = state.value.photos + (memory.entryId to (state.value.photos[memory.entryId].orEmpty() + updated.photos).distinctBy { it.id }))
    }

    override suspend fun deleteMemory(memoryId: String) = mutex.withLock {
        val family = home.load()
        check(!family.browsing)
        val memory = requireNotNull(state.value.memories.find { it.id == memoryId })
        check(memory.authorId == family.currentUserId)
        state.value = state.value.copy(memories = state.value.memories.filterNot { it.id == memoryId })
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RecordRepositoryModule {
    @Provides @Singleton
    fun provideRecordRepository(home: HomeRepository, missions: MissionRepository, details: DetailRepository): RecordRepository = FakeRecordRepository(home, missions, details)
}
