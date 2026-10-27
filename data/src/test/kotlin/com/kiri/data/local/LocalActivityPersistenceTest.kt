package com.kiri.data.local

import com.kiri.data.mission.UnavailableMissionRepository
import com.kiri.data.record.*
import com.kiri.data.schedule.LocalScheduleRepository
import com.kiri.domain.detail.*
import com.kiri.domain.home.*
import com.kiri.domain.mission.*
import com.kiri.domain.place.*
import com.kiri.domain.record.*
import com.kiri.domain.schedule.*
import com.kiri.domain.time.kiriToday
import java.io.IOException
import java.time.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LocalActivityPersistenceTest {
    private class MutableClock(
        var at: Instant = Instant.parse("2026-10-25T03:00:00Z"),
        private val zone: ZoneId = ZoneOffset.UTC,
    ) : Clock() {
        override fun getZone() = zone

        override fun instant() = at

        override fun withZone(zone: ZoneId): Clock = Clock.fixed(at, zone)
    }

    private class Home(val clock: Clock) : HomeRepository {
        var user = "1"
        var familyId: String? = "family-a"
        var familyName = "동일한 이름"
        var members = listOf(FamilyMember("1", "나", ""), FamilyMember("2", "가족", ""))
        var fail = false
        var activities: List<HomeSchedule> = emptyList()

        override suspend fun load(): FamilyHome {
            if (fail) throw IOException("연결 실패")
            val today = clock.kiriToday()
            return FamilyHome(
                today,
                user,
                familyName,
                members,
                DailyQuestion("q-$today", today, "질문"),
                mapOf(user to "내 답변"),
                emptySet(),
                null,
                schedules = activities,
                serverBacked = true,
                familyId = familyId,
            )
        }

        override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome =
            error("unused")

        override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome =
            error("unused")
    }

    private class Store : LocalStateStore {
        val memory = MemoryLocalStateStore()
        var failPrefix: String? = null

        override suspend fun read(key: String) = memory.read(key)

        override suspend fun write(key: String, value: String) {
            if (failPrefix?.let(key::startsWith) == true) throw IOException("저장 실패")
            memory.write(key, value)
        }
    }

    private val content =
        MissionContent(
            PlaceMission(
                "real-place",
                "저장한 활동",
                "실제 장소",
                "서울",
                PlaceCategory.OUTDOOR,
                tags = listOf("태그"),
                searchMetadata = PlaceSearchMetadata("서울", 1.5, 100, 3, 1, 99999),
            ),
            1000,
            60,
            listOf(DetailSuggestion("산책", "설명")),
            listOf(DetailSuggestion("물", "준비물")),
            listOf(NearbyPlace("near", "쉼터", "주소", "설명", 100, true, 0, 1)),
            listOf(MissionReview("review", "작성자", "기록", 1)),
            MissionScheduleWindow(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                LocalTime.of(9, 0),
                LocalTime.of(23, 0),
            ),
        )
    private val missions =
        object : MissionRepository {
            override suspend fun catalog() = listOf(content.summary)

            override suspend fun load(missionId: String) = content.also {
                require(missionId == it.summary.id)
            }
        }

    private class AcceptedSchedules(clock: Clock) : ScheduleRepository {
        override val schedules =
            MutableStateFlow(
                listOf(
                    FamilySchedule(
                        "activity",
                        null,
                        "real-place",
                        "저장한 활동",
                        "1",
                        clock.instant().minusSeconds(3600),
                        clock.instant().minusSeconds(7200),
                        mapOf("1" to MemberResponse.ACCEPTED, "2" to MemberResponse.ACCEPTED),
                        "실제 장소",
                    )
                )
            )
        override val notices = MutableStateFlow(emptyList<ScheduleNotice>())
        var failComplete = false

        override suspend fun refresh() {}

        override suspend fun complete(id: String) {
            if (failComplete) throw IOException("완료 저장 실패")
            schedules.value =
                schedules.value.map { if (it.id == id) it.copy(completed = true) else it }
        }

        override suspend fun create(draft: ScheduleDraft, requestId: String): FamilySchedule =
            error("unused")

        override suspend fun update(
            id: String,
            draft: ScheduleDraft,
            expectedRevision: Int,
        ): FamilySchedule = error("unused")

        override suspend fun respond(
            id: String,
            response: MemberResponse,
            expectedRevision: Int,
        ): FamilySchedule = error("unused")

        override suspend fun cancel(id: String, expectedRevision: Int) = error("unused")
    }

    @Test
    fun bucketRestartsAreIdempotentAndAccountsNeverShareSaves() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val first = LocalBucketRepository(home, missions, clock, store)
        val bucket = first.save("real-place")
        val restarted = LocalBucketRepository(home, missions, clock, store)
        assertEquals(bucket, restarted.save("real-place"))
        home.user = "2"
        assertTrue(restarted.mine().isEmpty())
        val other = restarted.save("real-place")
        assertNotEquals(bucket.id, other.id)
        assertTrue(runCatching { restarted.delete(bucket.id) }.isFailure)
        home.user = "1"
        assertEquals(listOf(bucket), restarted.mine())
        restarted.delete(bucket.id)
        assertTrue(LocalBucketRepository(home, missions, clock, store).mine().isEmpty())
    }

    @Test
    fun familyIdentityNotNameControlsIsolationAndUnknownMembershipFailsClosed() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val repo = LocalBucketRepository(home, missions, clock, store)
        val bucket = repo.save("real-place")
        home.familyName = "새 이름"
        assertEquals(listOf(bucket), repo.mine())
        home.familyId = "family-b"
        assertTrue(repo.mine().isEmpty())
        home.familyId = "family-a"
        assertEquals(listOf(bucket), repo.mine())
        home.members = emptyList()
        assertTrue(repo.mine().isEmpty())
        assertTrue(runCatching { repo.save("real-place") }.isFailure)
    }

    @Test
    fun membershipFallbackChangesNamespaceAndDoesNotDeletePreviousData() = runTest {
        val clock = MutableClock()
        val home = Home(clock).apply { familyId = null }
        val store = Store()
        val repo = LocalBucketRepository(home, missions, clock, store)
        val bucket = repo.save("real-place")
        val previous = home.members
        home.members += FamilyMember("3", "새 가족", "")
        assertTrue(repo.mine().isEmpty())
        home.members = previous
        assertEquals(listOf(bucket), repo.mine())
    }

    @Test
    fun scheduleAndRequestIdsSurviveRestartWithoutDuplicateInvitations() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val buckets = LocalBucketRepository(home, missions, clock, store)
        val bucket = buckets.save("real-place")
        val draft =
            ScheduleDraft(
                bucket.id,
                bucket.missionId,
                clock.instant().plusSeconds(86400),
                setOf("2"),
                "메시지",
                "장소",
            )
        val original =
            LocalScheduleRepository(home, missions, buckets, clock, store).create(draft, "request")
        val restartedBuckets = LocalBucketRepository(home, missions, clock, store)
        val restarted = LocalScheduleRepository(home, missions, restartedBuckets, clock, store)
        assertEquals(original, restarted.create(draft, "request"))
        assertEquals(1, restarted.notices.value.size)
        restarted.cancel(original.id, original.revision)
        val final = LocalScheduleRepository(home, missions, restartedBuckets, clock, store)
        final.refresh()
        assertTrue(final.schedules.value.single().cancelled)
        assertTrue(restartedBuckets.mine().single().linkedScheduleIds.isEmpty())
        home.user = "2"
        final.refresh()
        assertTrue(final.schedules.value.isEmpty())
        assertTrue(final.notices.value.isEmpty())
    }

    @Test
    fun failedScheduleWriteRollsBackBucketLinkAndCanRetrySameRequest() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val buckets = LocalBucketRepository(home, missions, clock, store)
        val bucket = buckets.save("real-place")
        val repo = LocalScheduleRepository(home, missions, buckets, clock, store)
        val draft =
            ScheduleDraft(
                bucket.id,
                bucket.missionId,
                clock.instant().plusSeconds(86400),
                setOf("2"),
                "",
                "장소",
            )
        store.failPrefix = "schedules/"
        assertTrue(runCatching { repo.create(draft, "request") }.isFailure)
        assertTrue(repo.schedules.value.isEmpty())
        assertTrue(repo.notices.value.isEmpty())
        assertTrue(buckets.mine().single().linkedScheduleIds.isEmpty())
        store.failPrefix = null
        assertNotNull(repo.create(draft, "request"))
    }

    @Test
    fun unavailableCatalogueAndRealRecordRepositoryNeverSeedDemoContent() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        assertTrue(UnavailableMissionRepository().catalog().isEmpty())
        assertTrue(runCatching { UnavailableMissionRepository().load("place-river") }.isFailure)
        val repo = LocalRecordRepository(home, UnavailableMissionRepository(), clock = clock)
        repo.refresh()
        assertTrue(repo.records.value.entries.isEmpty())
        assertTrue(repo.records.value.memories.isEmpty())
        assertTrue(repo.records.value.photos.isEmpty())
        assertEquals("1", repo.records.value.currentUserId)
    }

    @Test
    fun questionHistorySurvivesRestartAndIsNotVisibleToAnotherAccount() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        LocalRecordRepository(home, missions, clock = clock, store = store).refresh()
        val before = clock.kiriToday()
        clock.at = clock.at.plusSeconds(86400)
        val repo = LocalRecordRepository(home, missions, clock = clock, store = store)
        repo.refresh()
        assertEquals(setOf(before, clock.kiriToday()), repo.records.value.questions.keys)
        home.user = "2"
        repo.refresh()
        assertEquals(setOf(clock.kiriToday()), repo.records.value.questions.keys)
        assertEquals("2", repo.records.value.currentUserId)
        assertFalse(
            repo.records.value.questions.getValue(clock.kiriToday()).answers.containsKey("1")
        )
    }

    @Test
    fun completeRecordSnapshotAndPhotoSelectionSurviveRestartAndDeletion() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val schedules = AcceptedSchedules(clock)
        val repo =
            LocalRecordRepository(
                home,
                missions,
                familySchedules = schedules,
                clock = clock,
                store = store,
            )
        repo.refresh()
        repo.addMemory(
            "activity",
            "추억",
            listOf(RecordPhoto("p1", "content://test/1"), RecordPhoto("p2", "content://test/2")),
            5,
            true,
        )
        repo.changeRepresentativePhoto("activity", "p2")
        val restarted =
            LocalRecordRepository(
                home,
                UnavailableMissionRepository(),
                familySchedules = schedules,
                clock = clock,
                store = store,
            )
        restarted.refresh()
        assertEquals("실제 장소", restarted.detail("activity").place)
        assertEquals(content, restarted.records.value.entries.single().missionSnapshot)
        assertEquals("p2", restarted.records.value.entries.single().representativePhotoId)
        val memory = restarted.records.value.memories.single()
        restarted.updateMemory(memory.copy(text = "수정", photos = memory.photos.take(1)))
        restarted.deleteMemory(memory.id)
        val last =
            LocalRecordRepository(
                home,
                UnavailableMissionRepository(),
                familySchedules = schedules,
                clock = clock,
                store = store,
            )
        last.refresh()
        assertTrue(last.records.value.memories.isEmpty())
        assertTrue(last.records.value.entries.single().completed)
        assertNull(last.records.value.entries.single().representativePhotoId)
    }

    @Test
    fun failedRecordWriteDoesNotPublishMemoryOrCompleteSchedule() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val schedules = AcceptedSchedules(clock)
        val repo =
            LocalRecordRepository(
                home,
                missions,
                familySchedules = schedules,
                clock = clock,
                store = store,
            )
        repo.refresh()
        store.failPrefix = "records/"
        assertTrue(
            runCatching {
                    repo.addMemory(
                        "activity",
                        "추억",
                        listOf(RecordPhoto("p1", "content://test/1")),
                        5,
                        true,
                    )
                }
                .isFailure
        )
        assertTrue(repo.records.value.memories.isEmpty())
        assertFalse(schedules.schedules.value.single().completed)
    }

    @Test
    fun interruptedCompletionIsRepairedFromDurableReviewWithoutDuplicateEntry() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val schedules = AcceptedSchedules(clock)
        val repo =
            LocalRecordRepository(
                home,
                missions,
                familySchedules = schedules,
                clock = clock,
                store = store,
            )
        repo.refresh()
        schedules.failComplete = true
        assertTrue(
            runCatching {
                    repo.addMemory(
                        "activity",
                        "추억",
                        listOf(RecordPhoto("p1", "content://test/1")),
                        5,
                        true,
                    )
                }
                .isFailure
        )
        schedules.failComplete = false
        val restarted =
            LocalRecordRepository(
                home,
                missions,
                familySchedules = schedules,
                clock = clock,
                store = store,
            )
        restarted.refresh()
        assertTrue(schedules.schedules.value.single().completed)
        assertEquals(1, restarted.records.value.entries.size)
        assertEquals(1, restarted.records.value.memories.size)
    }

    @Test
    fun authenticationFailureClearsVisibleStateAndCorruptDataIsNotOverwritten() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val buckets = LocalBucketRepository(home, missions, clock, store)
        buckets.save("real-place")
        home.fail = true
        assertTrue(runCatching { buckets.refresh() }.isFailure)
        assertTrue(buckets.buckets.value.isEmpty())
        home.fail = false
        val key = "buckets/${home.load().localStorageScope()}"
        store.write(key, "invalid-json")
        assertTrue(runCatching { buckets.refresh() }.isFailure)
        assertTrue(buckets.buckets.value.isEmpty())
        assertEquals("invalid-json", store.read(key))
    }

    @Test
    fun failedCancellationWriteLeavesItsBucketLinkedToTheActiveSchedule() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val buckets = LocalBucketRepository(home, missions, clock, store)
        val bucket = buckets.save("real-place")
        val repo = LocalScheduleRepository(home, missions, buckets, clock, store)
        val schedule =
            repo.create(
                ScheduleDraft(
                    bucket.id,
                    bucket.missionId,
                    clock.instant().plusSeconds(86400),
                    setOf("2"),
                    "",
                    "장소",
                ),
                "request",
            )
        store.failPrefix = "schedules/"
        assertTrue(runCatching { repo.cancel(schedule.id, schedule.revision) }.isFailure)
        assertFalse(repo.schedules.value.single().cancelled)
        assertEquals(setOf(schedule.id), buckets.mine().single().linkedScheduleIds)
    }

    @Test
    fun interruptedCancellationUnlinkIsRepairedOnNextRefresh() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val buckets = LocalBucketRepository(home, missions, clock, store)
        val bucket = buckets.save("real-place")
        val repo = LocalScheduleRepository(home, missions, buckets, clock, store)
        val schedule =
            repo.create(
                ScheduleDraft(
                    bucket.id,
                    bucket.missionId,
                    clock.instant().plusSeconds(86400),
                    setOf("2"),
                    "",
                    "장소",
                ),
                "request",
            )
        store.failPrefix = "buckets/"
        assertTrue(runCatching { repo.cancel(schedule.id, schedule.revision) }.isFailure)
        assertTrue(repo.schedules.value.single().cancelled)
        store.failPrefix = null
        val restartedBuckets = LocalBucketRepository(home, missions, clock, store)
        val restarted = LocalScheduleRepository(home, missions, restartedBuckets, clock, store)
        restarted.refresh()
        assertTrue(restarted.schedules.value.single().cancelled)
        assertTrue(restartedBuckets.mine().single().linkedScheduleIds.isEmpty())
    }

    @Test
    fun detailCustomizationsPersistAndRemainAccountScoped() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        home.activities =
            listOf(
                HomeSchedule(
                    "activity",
                    "활동",
                    clock.instant(),
                    ScheduleStatus.CONFIRMED,
                    true,
                    missionId = "real-place",
                )
            )
        val first =
            com.kiri.data.detail.LocalDetailRepository(home, missions, clock = clock, store = store)
        first.applyConditions("activity", setOf(Condition.MORE_REST))
        val restarted =
            com.kiri.data.detail.LocalDetailRepository(home, missions, clock = clock, store = store)
        assertEquals(setOf(Condition.MORE_REST), restarted.load("activity").conditions)
        home.user = "2"
        assertTrue(restarted.load("activity").conditions.isEmpty())
        home.user = "1"
        assertEquals(setOf(Condition.MORE_REST), restarted.load("activity").conditions)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentAccountSwitchCannotWriteAnotherAccountsMemoryIntoTheNewScope() = runTest {
        val clock = MutableClock()
        val home = Home(clock)
        val store = Store()
        val schedules = AcceptedSchedules(clock)
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val resume = kotlinx.coroutines.CompletableDeferred<Unit>()
        val media =
            object : RecordMediaStore {
                override suspend fun retain(
                    scope: String,
                    photos: List<RecordPhoto>,
                ): List<RecordPhoto> {
                    started.complete(Unit)
                    resume.await()
                    return photos
                }
            }
        val repo =
            LocalRecordRepository(
                home,
                missions,
                familySchedules = schedules,
                clock = clock,
                store = store,
                media = media,
            )
        repo.refresh()
        val scopeA = home.load().localStorageScope()
        val writer = async {
            repo.addMemory(
                "activity",
                "계정 A의 추억",
                listOf(RecordPhoto("p1", "content://test/1")),
                5,
                true,
            )
        }
        started.await()
        home.user = "2"
        val scopeB = home.load().localStorageScope()
        val switching = async {
            runCatching { repo.applyConditions("activity", setOf(Condition.MORE_REST)) }
        }
        runCurrent()
        resume.complete(Unit)
        writer.await()
        assertTrue(switching.await().isFailure)
        assertEquals(
            "1",
            ActivityCodec.records(requireNotNull(store.read("records/$scopeA")))
                .memories
                .single()
                .authorId,
        )
        assertNull(store.read("records/$scopeB"))
        repo.refresh()
        assertTrue(repo.records.value.memories.isEmpty())
        home.user = "1"
        repo.refresh()
        assertEquals("계정 A의 추억", repo.records.value.memories.single().text)
    }
}
