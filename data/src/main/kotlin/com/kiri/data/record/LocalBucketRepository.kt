package com.kiri.data.record

import com.kiri.domain.home.HomeRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.util.UUID

/** Process-local adapter, consistent with the app's existing demo repositories. */
class LocalBucketRepository(private val home: HomeRepository, private val missions: MissionRepository, private val clock: Clock) : BucketRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<FamilyBucket>>(emptyList())
    override val buckets = state.asStateFlow()
    private var familyName: String? = null
    override suspend fun refresh() = mutex.withLock { checkFamily() }
    override suspend fun mine(): List<FamilyBucket> = mutex.withLock {
        checkFamily()
        val family = home.load()
        if (family.browsing) emptyList() else state.value.filter { it.ownerId == family.currentUserId }
    }
    private suspend fun checkFamily() {
        val family = home.load()
        if (family.familyName != familyName) { state.value = emptyList(); familyName = family.familyName }
    }
    override suspend fun save(missionId: String): FamilyBucket = mutex.withLock {
        checkFamily()
        val family = home.load()
        check(!family.browsing) { "가족 연결 후 저장할 수 있어요." }
        missions.load(missionId)
        state.value.find { it.ownerId == family.currentUserId && it.missionId == missionId } ?: FamilyBucket(
            UUID.randomUUID().toString(), missionId, family.currentUserId, clock.instant(),
        ).also { state.value += it }
    }
    override suspend fun delete(id: String) = mutex.withLock {
        val family = home.load()
        check(!family.browsing)
        val bucket = requireNotNull(state.value.find { it.id == id })
        check(bucket.ownerId == family.currentUserId) { "내가 저장한 버킷만 삭제할 수 있어요." }
        check(bucket.linkedScheduleIds.isEmpty()) { "연결된 약속을 먼저 취소해주세요." }
        check(!bucket.completed) { "완료한 활동은 기록으로 남겨요." }
        state.value = state.value.filterNot { it.id == id }
    }
    override suspend fun link(id: String, scheduleId: String) = mutex.withLock {
        val bucket = requireNotNull(state.value.find { it.id == id })
        check(!bucket.completed && bucket.linkedScheduleIds.isEmpty()) { "이미 일정이 연결되었거나 완료한 버킷이에요." }
        state.value = state.value.map { if (it.id == id) it.copy(linkedScheduleIds = setOf(scheduleId)) else it }
    }
    override suspend fun unlink(id: String, scheduleId: String) = mutex.withLock {
        state.value = state.value.map { if (it.id == id) it.copy(linkedScheduleIds = it.linkedScheduleIds - scheduleId) else it }
    }
    override suspend fun complete(id: String) = mutex.withLock {
        state.value = state.value.map { if (it.id == id) it.copy(completed = true) else it }
    }
}
