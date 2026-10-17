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
)

data class RecordPhoto(val id: String, val source: String)
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

data class RecordCollection(
    val entries: List<CalendarEntry> = emptyList(),
    val memories: List<FamilyMemory> = emptyList(),
    val photos: Map<String, List<RecordPhoto>> = emptyMap(),
    val familyName: String = "",
    val currentUserId: String = "",
    val savers: Map<String, String> = emptyMap(),
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
}
