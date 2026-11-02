package com.kiri.data.record

import com.kiri.data.local.*
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.*
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Account-scoped local saves. These are not server-synchronized family buckets. */
class LocalBucketRepository(
    private val home: HomeRepository,
    private val missions: MissionRepository,
    private val clock: Clock,
    private val store: LocalStateStore = MemoryLocalStateStore(),
) : BucketRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<FamilyBucket>>(emptyList())
    override val buckets = state.asStateFlow()
    private var scope: String? = null

    override suspend fun refresh() {
        mutex.withLock { family() }
    }

    override suspend fun mine(): List<FamilyBucket> = mutex.withLock {
        val family = family()
        state.value.filter { it.ownerId == family.currentUserId }
    }

    private suspend fun family(): FamilyHome {
        try {
            val family = home.load()
            val next = family.localStorageScope()
            if (next != scope) {
                scope = null
                state.value = emptyList()
                val restored =
                    next?.let { store.read("buckets/$it") }?.let(ActivityCodec::buckets).orEmpty()
                state.value = restored
                scope = next
            }
            return family
        } catch (error: Exception) {
            scope = null
            state.value = emptyList()
            throw error
        }
    }

    private suspend fun publish(value: List<FamilyBucket>) {
        store.write("buckets/${requireNotNull(scope)}", ActivityCodec.buckets(value))
        state.value = value
    }

    override suspend fun save(missionId: String): FamilyBucket = mutex.withLock {
        val family = family()
        check(scope != null) { "가족 연결 후 저장할 수 있어요." }
        missions.load(missionId)
        state.value.find {
            it.ownerId == family.currentUserId && it.missionId == missionId && !it.completed
        }
            ?: FamilyBucket(
                    UUID.randomUUID().toString(),
                    missionId,
                    family.currentUserId,
                    clock.instant(),
                )
                .also { publish(state.value + it) }
    }

    /** Called only on final send. Bucket creation/link and schedule persistence share one edit. */
    internal suspend fun commitWeeklySchedule(
        missionId: String,
        scheduleId: String,
        expectedScope: String,
        weeklyMissionId: String,
        transactionStore: LocalStateStore,
        snapshots: (FamilyBucket) -> Map<String, String>,
    ): FamilyBucket = mutex.withLock {
        val family = family()
        check(store === transactionStore) { "일정과 버킷은 같은 저장소를 사용해야 해요." }
        check(scope == expectedScope) { "가족 정보가 변경되었어요. 다시 시도해주세요." }
        check(family.mission?.let {
            it.id == weeklyMissionId && it.missionId == missionId && !it.completed && !it.unavailable
        } == true) { "이번 주 미션이 변경되었어요. 홈에서 다시 확인해주세요." }
        missions.load(missionId)
        val bucket = state.value.find {
            it.ownerId == family.currentUserId && it.missionId == missionId && !it.completed
        } ?: FamilyBucket(UUID.randomUUID().toString(), missionId, family.currentUserId, clock.instant())
        check(bucket.canCreateSchedule) { "이미 일정이 연결된 버킷이에요." }
        val linked = bucket.copy(linkedScheduleIds = setOf(scheduleId))
        val updated = state.value.filterNot { it.id == bucket.id } + linked
        val writes = snapshots(linked) + ("buckets/$expectedScope" to ActivityCodec.buckets(updated))
        store.writeAtomically(writes)
        state.value = updated
        linked
    }

    override suspend fun delete(id: String) = mutex.withLock {
        val family = family()
        check(scope != null)
        val bucket = requireNotNull(state.value.find { it.id == id })
        check(bucket.ownerId == family.currentUserId) { "내가 저장한 버킷만 삭제할 수 있어요." }
        check(bucket.linkedScheduleIds.isEmpty()) { "연결된 약속을 먼저 취소해주세요." }
        check(!bucket.completed) { "완료한 활동은 기록으로 남겨요." }
        publish(state.value.filterNot { it.id == id })
    }

    override suspend fun link(id: String, scheduleId: String) = mutex.withLock {
        family()
        val bucket = requireNotNull(state.value.find { it.id == id })
        check(bucket.canCreateSchedule) {
            "이미 일정이 연결되었거나 완료한 버킷이에요."
        }
        publish(
            state.value.map {
                if (it.id == id) it.copy(linkedScheduleIds = setOf(scheduleId)) else it
            }
        )
    }

    override suspend fun unlink(id: String, scheduleId: String) = mutex.withLock {
        family()
        publish(
            state.value.map {
                if (it.id == id) it.copy(linkedScheduleIds = it.linkedScheduleIds - scheduleId)
                else it
            }
        )
    }

    override suspend fun complete(id: String) = mutex.withLock {
        family()
        publish(state.value.map { if (it.id == id) it.copy(completed = true) else it })
    }
}
