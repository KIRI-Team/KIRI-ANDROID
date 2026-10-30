package com.kiri.domain.record

import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

data class FamilyBucket(
    val id: String,
    val missionId: String,
    val ownerId: String,
    val savedAt: Instant,
    val completed: Boolean = false,
    val linkedScheduleIds: Set<String> = emptySet(),
) {
    val status: BucketStatus
        get() = when {
            completed -> BucketStatus.COMPLETED
            linkedScheduleIds.isNotEmpty() -> BucketStatus.SCHEDULED
            else -> BucketStatus.SAVED
        }
    val canCreateSchedule get() = status == BucketStatus.SAVED
}

enum class BucketStatus { SAVED, SCHEDULED, COMPLETED }

/** Only unfinished saves are unique per member/mission. Completed buckets remain as history. */
interface BucketRepository {
    val buckets: StateFlow<List<FamilyBucket>>
    suspend fun refresh()
    suspend fun mine(): List<FamilyBucket>
    suspend fun save(missionId: String): FamilyBucket
    suspend fun delete(id: String)
    suspend fun link(id: String, scheduleId: String)
    suspend fun unlink(id: String, scheduleId: String)
    suspend fun complete(id: String)
}
