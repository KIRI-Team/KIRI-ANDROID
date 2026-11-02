package com.kiri.data.local

import com.kiri.domain.detail.*
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.mission.*
import com.kiri.domain.place.*
import com.kiri.domain.record.*
import com.kiri.domain.schedule.*
import java.time.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Storage DTOs keep serialization and disk schema out of the domain layer. */
internal object ActivityCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun conditions(value: Map<String, Set<Condition>>): String =
        json.encodeToString(
            ConditionFile(items = value.mapValues { (_, values) -> values.map { it.name }.toSet() })
        )

    fun conditions(value: String): Map<String, Set<Condition>> =
        json
            .decodeFromString<ConditionFile>(value)
            .also { require(it.version == 1) }
            .items
            .mapValues { (_, values) -> values.map(Condition::valueOf).toSet() }

    fun buckets(value: List<FamilyBucket>): String =
        json.encodeToString(
            BucketFile(
                items =
                    value.map {
                        StoredBucket(
                            it.id,
                            it.missionId,
                            it.ownerId,
                            it.savedAt.toString(),
                            it.completed,
                            it.linkedScheduleIds,
                        )
                    }
            )
        )

    fun buckets(value: String): List<FamilyBucket> =
        json
            .decodeFromString<BucketFile>(value)
            .also { require(it.version == 1) }
            .items
            .map {
                FamilyBucket(
                    it.id,
                    it.missionId,
                    it.ownerId,
                    Instant.parse(it.savedAt),
                    it.completed,
                    it.links,
                )
            }

    fun schedules(value: ScheduleState): String =
        json.encodeToString(
            ScheduleFile(
                items =
                    value.schedules.map {
                        StoredSchedule(
                            it.id,
                            it.bucketId,
                            it.missionId,
                            it.title,
                            it.proposerId,
                            it.startsAt.toString(),
                            it.responseDeadline.toString(),
                            it.responses.mapValues { e -> e.value.name },
                            it.place,
                            it.message,
                            it.revision,
                            it.cancelled,
                            it.completed,
                            it.weeklyMissionId,
                        )
                    },
                notices =
                    value.notices.map {
                        StoredNotice(
                            it.id,
                            it.scheduleId,
                            it.recipientIds,
                            it.message,
                            it.createdAt.toString(),
                        )
                    },
                requests =
                    value.requests.mapValues { (_, pair) ->
                        val d = pair.first
                        StoredRequest(
                            pair.second,
                            d.bucketId,
                            d.missionId,
                            d.startsAt?.toString(),
                            d.inviteeIds,
                            d.message,
                            d.place,
                            d.weeklyMissionId,
                        )
                    },
            )
        )

    fun schedules(value: String): ScheduleState =
        json
            .decodeFromString<ScheduleFile>(value)
            .also { require(it.version == 1) }
            .let { file ->
                ScheduleState(
                    file.items.map {
                        FamilySchedule(
                            it.id,
                            it.bucketId,
                            it.missionId,
                            it.title,
                            it.proposerId,
                            Instant.parse(it.startsAt),
                            Instant.parse(it.deadline),
                            it.responses.mapValues { e -> MemberResponse.valueOf(e.value) },
                            it.place,
                            it.message,
                            it.revision,
                            it.cancelled,
                            it.completed,
                            it.weeklyMissionId,
                        )
                    },
                    file.notices.map {
                        ScheduleNotice(
                            it.id,
                            it.scheduleId,
                            it.recipients,
                            it.message,
                            Instant.parse(it.at),
                        )
                    },
                    file.requests.mapValues { (_, r) ->
                        ScheduleDraft(
                            r.bucketId,
                            r.missionId,
                            r.startsAt?.let(Instant::parse),
                            r.invitees,
                            r.message,
                            r.place,
                            r.weeklyMissionId,
                        ) to r.id
                    },
                )
            }

    fun records(value: RecordCollection): String =
        json.encodeToString(
            RecordFile(
                entries =
                    value.entries.map {
                        StoredEntry(
                            it.id,
                            it.missionId,
                            it.date.toString(),
                            it.kind.name,
                            it.title,
                            it.startTime?.toString(),
                            it.saverId,
                            it.participantIds,
                            it.representativePhotoId,
                            it.conditions.map { c -> c.name }.toSet(),
                            it.completed,
                            it.linkedScheduleIds,
                            it.status.name,
                            it.missionSnapshot?.stored(),
                            it.placeSnapshot,
                        )
                    },
                memories =
                    value.memories.map {
                        StoredMemory(
                            it.id,
                            it.entryId,
                            it.authorId,
                            it.authorName,
                            it.role,
                            it.text,
                            it.photos.map { p -> StoredPhoto(p.id, p.source) },
                            it.rating,
                            it.repeatActivity,
                        )
                    },
                photos =
                    value.photos.mapValues { (_, photos) ->
                        photos.map { StoredPhoto(it.id, it.source) }
                    },
                familyName = value.familyName,
                userId = value.currentUserId,
                savers = value.savers,
                questions =
                    value.questions.values.map {
                        StoredQuestion(
                            it.date.toString(),
                            it.text,
                            it.answers,
                            it.memberNames,
                            it.everyoneAnswered,
                        )
                    },
            )
        )

    fun records(value: String): RecordCollection =
        json
            .decodeFromString<RecordFile>(value)
            .also { require(it.version == 1) }
            .let { file ->
                RecordCollection(
                    file.entries.map {
                        CalendarEntry(
                            it.id,
                            it.missionId,
                            LocalDate.parse(it.date),
                            CalendarEntryKind.valueOf(it.kind),
                            it.title,
                            it.time?.let(LocalTime::parse),
                            it.saverId,
                            it.participants,
                            it.photoId,
                            it.conditions.map(Condition::valueOf).toSet(),
                            it.completed,
                            it.links,
                            ScheduleStatus.valueOf(it.status),
                            it.mission?.domain(),
                            it.place,
                        )
                    },
                    file.memories.map {
                        FamilyMemory(
                            it.id,
                            it.entryId,
                            it.authorId,
                            it.authorName,
                            it.role,
                            it.text,
                            it.photos.map { p -> RecordPhoto(p.id, p.source) },
                            it.rating,
                            it.repeat,
                        )
                    },
                    file.photos.mapValues { (_, photos) ->
                        photos.map { RecordPhoto(it.id, it.source) }
                    },
                    file.familyName,
                    file.userId,
                    file.savers,
                    file.questions.associate {
                        LocalDate.parse(it.date) to
                            RecordedQuestion(
                                LocalDate.parse(it.date),
                                it.text,
                                it.answers,
                                it.names,
                                it.everyoneAnswered,
                            )
                    },
                )
            }
}

internal data class ScheduleState(
    val schedules: List<FamilySchedule> = emptyList(),
    val notices: List<ScheduleNotice> = emptyList(),
    val requests: Map<String, Pair<ScheduleDraft, String>> = emptyMap(),
)

@Serializable
private data class ConditionFile(val version: Int = 1, val items: Map<String, Set<String>>)

@Serializable private data class BucketFile(val version: Int = 1, val items: List<StoredBucket>)

@Serializable
private data class StoredBucket(
    val id: String,
    val missionId: String,
    val ownerId: String,
    val savedAt: String,
    val completed: Boolean,
    val links: Set<String>,
)

@Serializable
private data class ScheduleFile(
    val version: Int = 1,
    val items: List<StoredSchedule>,
    val notices: List<StoredNotice>,
    val requests: Map<String, StoredRequest>,
)

@Serializable
private data class StoredSchedule(
    val id: String,
    val bucketId: String?,
    val missionId: String,
    val title: String,
    val proposerId: String,
    val startsAt: String,
    val deadline: String,
    val responses: Map<String, String>,
    val place: String,
    val message: String,
    val revision: Int,
    val cancelled: Boolean,
    val completed: Boolean,
    val weeklyMissionId: String?,
)

@Serializable
private data class StoredNotice(
    val id: String,
    val scheduleId: String,
    val recipients: Set<String>,
    val message: String,
    val at: String,
)

@Serializable
private data class StoredRequest(
    val id: String,
    val bucketId: String,
    val missionId: String,
    val startsAt: String?,
    val invitees: Set<String>,
    val message: String,
    val place: String,
    val weeklyMissionId: String? = null,
)

@Serializable
private data class RecordFile(
    val version: Int = 1,
    val entries: List<StoredEntry>,
    val memories: List<StoredMemory>,
    val photos: Map<String, List<StoredPhoto>>,
    val familyName: String,
    val userId: String,
    val savers: Map<String, String>,
    val questions: List<StoredQuestion>,
)

@Serializable
private data class StoredEntry(
    val id: String,
    val missionId: String,
    val date: String,
    val kind: String,
    val title: String,
    val time: String?,
    val saverId: String?,
    val participants: List<String>,
    val photoId: String?,
    val conditions: Set<String>,
    val completed: Boolean,
    val links: Set<String>,
    val status: String,
    val mission: StoredMission?,
    val place: String?,
)

@Serializable private data class StoredPhoto(val id: String, val source: String)

@Serializable
private data class StoredMemory(
    val id: String,
    val entryId: String,
    val authorId: String,
    val authorName: String,
    val role: String,
    val text: String,
    val photos: List<StoredPhoto>,
    val rating: Int,
    val repeat: Boolean?,
)

@Serializable
private data class StoredQuestion(
    val date: String,
    val text: String,
    val answers: Map<String, String>,
    val names: Map<String, String>,
    val everyoneAnswered: Boolean,
)

@Serializable
private data class StoredSummary(
    val id: String,
    val title: String,
    val place: String,
    val region: String,
    val category: String,
    val rating: String?,
    val tags: List<String>,
    val pick: Boolean,
    val saved: Boolean,
    val facts: StoredSearch?,
)

@Serializable
private data class StoredSearch(
    val region: String,
    val distanceKm: Double,
    val price: Int,
    val capacity: Int,
    val from: Long,
    val through: Long,
    val opens: Int,
    val closes: Int,
)

@Serializable private data class StoredSuggestion(val title: String, val description: String)

@Serializable
private data class StoredNearby(
    val id: String,
    val title: String,
    val address: String,
    val description: String,
    val distance: Int?,
    val rest: Boolean?,
    val cost: Int?,
    val crowd: Int?,
)

@Serializable
private data class StoredReview(
    val id: String,
    val author: String,
    val text: String,
    val likes: Int,
)

@Serializable
private data class StoredWindow(
    val first: String,
    val last: String,
    val opens: String?,
    val closes: String?,
)

@Serializable
private data class StoredMission(
    val summary: StoredSummary,
    val steps: Int?,
    val minutes: Int?,
    val activities: List<StoredSuggestion>,
    val essentials: List<StoredSuggestion>,
    val nearby: List<StoredNearby>,
    val reviews: List<StoredReview>,
    val window: StoredWindow?,
)

private fun MissionContent.stored() =
    StoredMission(
        summary.let { s ->
            StoredSummary(
                s.id,
                s.title,
                s.place,
                s.region,
                s.category.name,
                s.rating,
                s.tags,
                s.pick,
                s.saved,
                s.searchMetadata?.let {
                    StoredSearch(
                        it.region,
                        it.distanceKm,
                        it.pricePerPerson,
                        it.capacity,
                        it.availableFromDay,
                        it.availableThroughDay,
                        it.opensAtMinute,
                        it.closesAtMinute,
                    )
                },
            )
        },
        expectedSteps,
        durationMinutes,
        activities.map { StoredSuggestion(it.title, it.description) },
        essentials.map { StoredSuggestion(it.title, it.description) },
        nearby.map {
            StoredNearby(
                it.id,
                it.title,
                it.address,
                it.description,
                it.distanceMeters,
                it.restAvailable,
                it.costWon,
                it.crowdLevel,
            )
        },
        reviews.map { StoredReview(it.id, it.author, it.text, it.likes) },
        scheduleWindow?.let {
            StoredWindow(
                it.firstDate.toString(),
                it.lastDate.toString(),
                it.opensAt?.toString(),
                it.closesAt?.toString(),
            )
        },
    )

private fun StoredMission.domain() =
    MissionContent(
        summary.let { s ->
            PlaceMission(
                s.id,
                s.title,
                s.place,
                s.region,
                PlaceCategory.valueOf(s.category),
                s.rating,
                s.tags,
                s.pick,
                s.saved,
                s.facts?.let {
                    PlaceSearchMetadata(
                        it.region,
                        it.distanceKm,
                        it.price,
                        it.capacity,
                        it.from,
                        it.through,
                        it.opens,
                        it.closes,
                    )
                },
            )
        },
        steps,
        minutes,
        activities.map { DetailSuggestion(it.title, it.description) },
        essentials.map { DetailSuggestion(it.title, it.description) },
        nearby.map {
            NearbyPlace(
                it.id,
                it.title,
                it.address,
                it.description,
                it.distance,
                it.rest,
                it.cost,
                it.crowd,
            )
        },
        reviews.map { MissionReview(it.id, it.author, it.text, it.likes) },
        window?.let {
            MissionScheduleWindow(
                LocalDate.parse(it.first),
                LocalDate.parse(it.last),
                it.opens?.let(LocalTime::parse),
                it.closes?.let(LocalTime::parse),
            )
        },
    )
