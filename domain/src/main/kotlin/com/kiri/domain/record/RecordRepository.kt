package com.kiri.domain.record

import com.kiri.domain.detail.Condition
import com.kiri.domain.detail.MissionDetail
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime

enum class CalendarEntryKind { SCHEDULE, BUCKET, MEMORY }

/** An occurrence belongs to a canonical mission, not to a copied mission title. */
data class CalendarEntry(
    val id: String,
    val missionId: String,
    val date: LocalDate,
    val kind: CalendarEntryKind,
    val title: String,
    val startTime: LocalTime? = null,
    val saverId: String? = null,
    val participantIds: List<String> = emptyList(),
    val representativePhotoId: String? = null,
    val conditions: Set<Condition> = emptySet(),
    val completed: Boolean = false,
    val linkedScheduleIds: Set<String> = emptySet(),
    val status: com.kiri.domain.home.ScheduleStatus = com.kiri.domain.home.ScheduleStatus.CONFIRMED,
    val missionSnapshot: com.kiri.domain.mission.MissionContent? = null,
    val placeSnapshot: String? = null,
)

enum class ScheduleRequestChannel { APP, KAKAO }

data class ScheduleRequest(
    val bucketId: String,
    val date: LocalDate,
    val time: LocalTime,
    val participantIds: Set<String>,
    val message: String,
    val channel: ScheduleRequestChannel,
    val requestId: String = "",
)

data class RecordPhoto(val id: String, val source: String)
data class RecordedQuestion(val date: LocalDate, val text: String, val answers: Map<String, String>, val memberNames: Map<String, String>, val everyoneAnswered: Boolean)
data class FamilyMemory(
    val id: String,
    val entryId: String,
    val authorId: String,
    val authorName: String,
    val role: String,
    val text: String,
    val photos: List<RecordPhoto> = emptyList(),
    val rating: Int = 0,
    val repeatActivity: Boolean? = null,
)

fun FamilyMemory.isValidReview(): Boolean = photos.isNotEmpty() && photos.all { it.source.isNotBlank() } &&
    text.isNotBlank() && text.length <= 500 && rating in 1..5 && repeatActivity != null

data class RecordCollection(
    val entries: List<CalendarEntry> = emptyList(),
    val memories: List<FamilyMemory> = emptyList(),
    val photos: Map<String, List<RecordPhoto>> = emptyMap(),
    val familyName: String = "",
    val currentUserId: String = "",
    val savers: Map<String, String> = emptyMap(),
    val questions: Map<LocalDate, RecordedQuestion> = emptyMap(),
)

/** Implementations enforce family access and author ownership before changing records. */
interface RecordRepository {
    val records: StateFlow<RecordCollection>
    suspend fun refresh()
    suspend fun detail(entryId: String): MissionDetail
    suspend fun applyConditions(entryId: String, conditions: Set<Condition>): MissionDetail
    suspend fun changeRepresentativePhoto(entryId: String, photoId: String)
    suspend fun updateMemory(memory: FamilyMemory)
    suspend fun deleteMemory(memoryId: String)
    suspend fun requestSchedule(request: ScheduleRequest): CalendarEntry { error("Schedule creation is unavailable") }
    suspend fun addMemory(entryId: String, text: String, photos: List<RecordPhoto>, rating: Int, repeatActivity: Boolean) {
        error("Review creation is unavailable")
    }
    suspend fun deleteBucket(bucketId: String) { error("Bucket deletion is unavailable") }
}
