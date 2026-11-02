package com.kiri.data.schedule

import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.local.*
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.*
import com.kiri.domain.home.*
import com.kiri.domain.record.*
import com.kiri.domain.schedule.*
import java.io.IOException
import java.time.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WeeklyMissionPolicyTest {
    private class TestClock(var at: Instant = Instant.parse("2026-10-18T03:00:00Z")) : Clock() {
        override fun instant() = at
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
    private class Store : LocalStateStore {
        private val memory = MemoryLocalStateStore()
        var fail = false
        var started: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        var resume: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        override suspend fun read(key: String) = memory.read(key)
        override suspend fun write(key: String, value: String) = memory.write(key, value)
        override suspend fun writeAtomically(values: Map<String, String>) {
            if (fail) throw IOException("disk full")
            started?.complete(Unit)
            resume?.await()
            memory.writeAtomically(values)
        }
    }
    private class Fixture {
        val clock = TestClock()
        val source = FakeHomeRepository(clock, latencyMillis = 0)
        var user = "me"
        var weeklyId = "weekly-demo"
        val home = object : HomeRepository by source {
            override suspend fun load(): FamilyHome {
                val loaded = source.load()
                return loaded.copy(currentUserId = user, schedules = emptyList(),
                    mission = loaded.mission?.copy(id = weeklyId))
            }
        }
        val missions = FakeMissionRepository()
        val store = Store()
        var buckets = LocalBucketRepository(home, missions, clock, store)
        var schedules = LocalScheduleRepository(home, missions, buckets, clock, store)
        suspend fun draft(): ScheduleDraft {
            val weekly = requireNotNull(home.load().mission)
            return ScheduleDraft("", weekly.missionId, clock.at.plusSeconds(3 * 86400),
                setOf("mother"), "함께 가요", "한강", weekly.id)
        }
        suspend fun restart() {
            buckets = LocalBucketRepository(home, missions, clock, store)
            schedules = LocalScheduleRepository(home, missions, buckets, clock, store)
            schedules.refresh()
        }
    }

    @Test fun finalSendCreatesOneBucketAndScheduleAndRetrySurvivesRestart() = runTest {
        val f = Fixture()
        val draft = f.draft()
        f.schedules.refresh()
        assertTrue(f.buckets.mine().isEmpty())
        val results = (1..4).map { async { f.schedules.create(draft, "send") } }.awaitAll()
        assertEquals(1, results.map { it.id }.distinct().size)
        val activity = results.first()
        assertEquals(draft.weeklyMissionId, activity.weeklyMissionId)
        assertEquals(setOf(activity.id), f.buckets.mine().single().linkedScheduleIds)
        f.restart()
        assertEquals(activity, f.schedules.create(draft, "send"))
        assertTrue(runCatching { f.schedules.create(draft, "duplicate") }.isFailure)
        assertEquals(1, f.buckets.mine().size)
        val place = com.kiri.data.place.LocalPlaceRepository(missionRepository = f.missions, familyBuckets = f.buckets)
        assertEquals("저장됨", place.load(com.kiri.domain.place.PlaceQuery()).items.first {
            it.id == draft.missionId
        }.saveLabel)
    }

    @Test fun existingRecordFlowRegistersWeeklyActivityWithoutPreSaving() = runTest {
        val f = Fixture(); val draft = f.draft()
        val records = LocalRecordRepository(f.home, f.missions, familySchedules = f.schedules,
            familyBuckets = f.buckets, clock = f.clock, store = f.store)
        records.refresh()
        assertTrue(f.buckets.mine().isEmpty())
        val local = requireNotNull(draft.startsAt).atZone(ZoneId.of("Asia/Seoul"))
        val request = ScheduleRequest("", local.toLocalDate(), local.toLocalTime(), setOf("mother"),
            "함께 가요", ScheduleRequestChannel.APP, "record-weekly", draft.weeklyMissionId, draft.missionId)
        val first = records.requestSchedule(request)
        assertEquals(first.id, records.requestSchedule(request).id)
        assertEquals(first.id, f.schedules.schedules.value.single().id)
        assertEquals(1, records.records.value.entries.count { it.kind == CalendarEntryKind.BUCKET })
        assertEquals(1, f.buckets.mine().size)
    }

    @Test fun failureLeavesNoBucketAndKeepsAnExistingSavedBucketUnchanged() = runTest {
        val f = Fixture()
        val draft = f.draft()
        f.store.fail = true
        assertTrue(runCatching { f.schedules.create(draft, "send") }.isFailure)
        assertTrue(f.buckets.mine().isEmpty())
        assertTrue(f.schedules.schedules.value.isEmpty())
        f.restart()
        assertTrue(f.buckets.mine().isEmpty())
        val saved = f.buckets.save(draft.missionId)
        assertTrue(runCatching { f.schedules.create(draft, "send") }.isFailure)
        assertEquals(saved, f.buckets.mine().single())
        f.store.fail = false
        val activity = f.schedules.create(draft, "send")
        assertEquals(saved.id, activity.bucketId)
        assertEquals(1, f.buckets.mine().size)
    }

    @Test fun leavingDuringFinalCommitKeepsTheBucketScheduleAndRetryConsistent() = runTest {
        val f = Fixture(); val draft = f.draft()
        f.store.started = kotlinx.coroutines.CompletableDeferred()
        f.store.resume = kotlinx.coroutines.CompletableDeferred()
        val sending = async { f.schedules.create(draft, "send") }
        f.store.started!!.await()
        sending.cancel()
        f.store.resume!!.complete(Unit)
        sending.join()
        val committed = f.schedules.schedules.value.single()
        assertEquals(setOf(committed.id), f.buckets.mine().single().linkedScheduleIds)
        assertEquals(committed, f.schedules.create(draft, "send"))
        f.restart()
        assertEquals(committed, f.schedules.create(draft, "send"))
    }

    @Test fun reusesOnlyOwnersIncompleteBucketAndNeverReusesCompletedHistory() = runTest {
        val f = Fixture(); val draft = f.draft()
        val completed = f.buckets.save(draft.missionId)
        f.buckets.complete(completed.id)
        f.user = "mother"
        val others = f.buckets.save(draft.missionId)
        f.user = "me"
        val activity = f.schedules.create(draft, "send")
        assertNotEquals(completed.id, activity.bucketId)
        assertNotEquals(others.id, activity.bucketId)
        assertEquals(completed.copy(completed = true), f.buckets.mine().first { it.id == completed.id })
        assertEquals(3, f.buckets.buckets.value.size)
    }

    @Test fun invalidOrChangedAssignmentAndAlreadyLinkedBucketCannotCreateAnything() = runTest {
        val f = Fixture(); val draft = f.draft()
        assertTrue(runCatching { f.schedules.create(draft.copy(inviteeIds = emptySet()), "bad") }.isFailure)
        f.weeklyId = "next-week"
        assertTrue(runCatching { f.schedules.create(draft, "stale") }.isFailure)
        assertTrue(f.buckets.mine().isEmpty())
        f.weeklyId = requireNotNull(draft.weeklyMissionId)
        val bucket = f.buckets.save(draft.missionId)
        f.schedules.create(draft.copy(bucketId = bucket.id, weeklyMissionId = null), "ordinary")
        assertTrue(runCatching { f.schedules.create(draft, "weekly") }.isFailure)
        assertEquals(1, f.buckets.mine().size)
        assertNull(f.schedules.schedules.value.single().weeklyMissionId)
    }

    @Test fun cancellationAllowsRetryAndParticipantReviewCompletesBucketAndWeeklyDetail() = runTest {
        val f = Fixture(); val draft = f.draft()
        val cancelled = f.schedules.create(draft, "first")
        f.schedules.cancel(cancelled.id, cancelled.revision)
        assertTrue(f.buckets.mine().single().canCreateSchedule)
        val current = f.schedules.create(draft, "second")
        assertEquals(cancelled.bucketId, current.bucketId)
        f.user = "mother"
        f.schedules.respond(current.id, MemberResponse.ACCEPTED, current.revision)
        f.clock.at = current.startsAt.plusSeconds(1)
        val records = LocalRecordRepository(f.home, f.missions, familySchedules = f.schedules,
            familyBuckets = f.buckets, clock = f.clock, store = f.store)
        records.refresh()
        assertTrue(runCatching { records.addMemory(current.id, "", emptyList(), 0, true) }.isFailure)
        assertFalse(f.schedules.schedules.value.first { it.id == current.id }.completed)
        records.addMemory(current.id, "즐거웠어요", listOf(RecordPhoto("p", "content://photo")), 5, true)
        assertTrue(f.schedules.schedules.value.first { it.id == current.id }.completed)
        assertTrue(f.buckets.buckets.value.single().completed)
        f.user = "me"
        val place = com.kiri.data.place.LocalPlaceRepository(missionRepository = f.missions, familyBuckets = f.buckets)
        assertEquals("다시 버킷에 저장", place.load(com.kiri.domain.place.PlaceQuery()).items.first {
            it.id == draft.missionId
        }.saveLabel)
        assertTrue(f.schedules.schedules.value.first { it.id == cancelled.id }.cancelled)
        assertEquals(CalendarEntryKind.MEMORY, records.records.value.entries.first { it.id == current.id }.kind)
        val detail = com.kiri.data.detail.LocalDetailRepository(f.home, f.missions, f.schedules, f.clock, f.store)
        assertTrue(detail.load(requireNotNull(draft.weeklyMissionId)).completed)
        assertEquals(1, records.records.value.memories.size)
        f.restart()
        assertTrue(f.schedules.schedules.value.first { it.id == current.id }.completed)
    }
}
