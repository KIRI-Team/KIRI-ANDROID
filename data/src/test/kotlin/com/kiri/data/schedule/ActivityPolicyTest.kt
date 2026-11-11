package com.kiri.data.schedule

import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.*
import com.kiri.domain.home.*
import com.kiri.domain.record.*
import com.kiri.domain.schedule.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ActivityPolicyTest {
    private class MutableClock(var time: Instant = Instant.parse("2026-10-18T03:00:00Z")) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = time
    }
    private class Fixture {
        val clock = MutableClock()
        val source = FakeHomeRepository(clock, latencyMillis = 0)
        var user = "me"
        val home = object : HomeRepository by source {
            override suspend fun load() = source.load().copy(currentUserId = user)
            override suspend fun family() = load()
        }
        val missions = FakeMissionRepository()
        val buckets = LocalBucketRepository(home, missions, clock)
        val schedules = LocalScheduleRepository(home, missions, buckets, clock)
        val records = FakeRecordRepository(home, missions, familySchedules = schedules, familyBuckets = buckets, clock = clock)
        suspend fun draft(): ScheduleDraft {
            schedules.refresh()
            val bucket = buckets.save("place-river")
            return ScheduleDraft(bucket.id, bucket.missionId, clock.instant().plusSeconds(3 * 86400), setOf("mother", "father"), "함께 가요", "한강")
        }
    }
    @Test fun deadlinesUse24HoursAndExactBoundaryClosesResponses() {
        val now = Instant.parse("2026-10-18T00:00:00Z")
        assertEquals(now.plusSeconds(86400), responseDeadline(now.plusSeconds(172800), now))
        assertEquals(now.plusSeconds(86400), responseDeadline(now.plusSeconds(86400), now))
        assertEquals(now.plusSeconds(3600), responseDeadline(now.plusSeconds(3600), now))
        assertTrue(runCatching { responseDeadline(now, now) }.isFailure)
    }
    @Test fun simultaneousSendRetriesCreateOneScheduleAndOnlySelectedResponseRows() = runTest {
        val f = Fixture(); val draft = f.draft()
        val schedules = (1..6).map { async { f.schedules.create(draft, "request") } }.awaitAll()
        assertEquals(1, schedules.map { it.id }.distinct().size)
        val schedule = schedules.first()
        assertEquals(setOf("me", "mother", "father"), schedule.responses.keys)
        assertEquals(MemberResponse.ACCEPTED, schedule.responses["me"])
        assertEquals(MemberResponse.PENDING, schedule.responses["mother"])
        assertEquals(1, f.schedules.notices.value.size)
        assertTrue(runCatching { f.schedules.create(draft, "another-request") }.isFailure)
    }
    @Test fun acceptDeclineChangesRecalculateGlobalStateAndNonInviteesCannotRespond() = runTest {
        val f = Fixture(); val original = f.schedules.create(f.draft(), "request")
        f.user = "sibling"
        assertTrue(runCatching { f.schedules.respond(original.id, MemberResponse.ACCEPTED, original.revision) }.isFailure)
        f.user = "mother"
        val accepted = f.schedules.respond(original.id, MemberResponse.ACCEPTED, original.revision)
        assertEquals(ScheduleStatus.CONFIRMED, accepted.status(f.clock.instant()))
        assertTrue(runCatching { f.schedules.respond(original.id, MemberResponse.DECLINED, original.revision) }.isFailure)
        val declined = f.schedules.respond(original.id, MemberResponse.DECLINED, accepted.revision)
        assertEquals(ScheduleStatus.PENDING_RESPONSE, declined.status(f.clock.instant()))
        f.clock.time = declined.responseDeadline
        assertEquals(ScheduleStatus.NOT_CONFIRMED, declined.status(f.clock.instant()))
        assertTrue(runCatching { f.schedules.respond(original.id, MemberResponse.ACCEPTED, declined.revision) }.isFailure)
        assertEquals(MemberResponse.PENDING, declined.responses["father"])
    }
    @Test fun coreChangesRequireReconfirmationButNotesKeepResponsesAndRemovedMembersLoseAccess() = runTest {
        val f = Fixture(); val draft = f.draft(); val created = f.schedules.create(draft, "request")
        f.user = "mother"
        val accepted = f.schedules.respond(created.id, MemberResponse.ACCEPTED, created.revision)
        f.user = "me"
        val note = f.schedules.update(created.id, draft.copy(message = "메모 변경"), accepted.revision)
        assertEquals(accepted.responses, note.responses)
        val changed = f.schedules.update(created.id, draft.copy(startsAt = draft.startsAt!!.plusSeconds(3600)), note.revision)
        assertEquals(MemberResponse.RECONFIRM_REQUIRED, changed.responses["mother"])
        assertEquals(MemberResponse.PENDING, changed.responses["father"])
        assertEquals(ScheduleStatus.PENDING_RESPONSE, changed.status(f.clock.instant()))
        val removed = f.schedules.update(created.id, draft.copy(inviteeIds = setOf("father")), changed.revision)
        assertFalse("mother" in removed.responses)
        f.user = "mother"
        assertTrue(runCatching { f.schedules.respond(created.id, MemberResponse.ACCEPTED, removed.revision) }.isFailure)
    }
    @Test fun bucketOwnerDeduplicationAndCancelUnlinkAreSharedAcrossFeatures() = runTest {
        val f = Fixture(); val draft = f.draft()
        assertEquals(draft.bucketId, f.buckets.save(draft.missionId).id)
        f.user = "mother"
        val mothers = f.buckets.save(draft.missionId)
        assertNotEquals(draft.bucketId, mothers.id)
        assertTrue(runCatching { f.buckets.delete(draft.bucketId) }.isFailure)
        // The proposer need not be the saver.
        val schedule = f.schedules.create(draft.copy(inviteeIds = setOf("me")), "request")
        f.user = "me"
        assertTrue(runCatching { f.buckets.delete(draft.bucketId) }.isFailure)
        assertTrue(runCatching { f.schedules.cancel(schedule.id, schedule.revision) }.isFailure)
        f.user = "mother"; f.schedules.cancel(schedule.id, schedule.revision)
        f.user = "me"; f.buckets.delete(draft.bucketId)
        assertEquals(listOf(mothers.id), f.buckets.buckets.value.map { it.id })
    }
    @Test fun validReviewCompletesOnceAndDeletingOrRemovingPhotosKeepsCompletionAndUpdatesRepresentative() = runTest {
        val f = Fixture(); val draft = f.draft(); val created = f.schedules.create(draft, "request")
        f.user = "mother"; val accepted = f.schedules.respond(created.id, MemberResponse.ACCEPTED, created.revision)
        f.user = "me"; f.records.refresh()
        assertTrue(runCatching { f.records.addMemory(created.id, "기록", listOf(RecordPhoto("photo", "content://photo")), 5, true) }.isFailure)
        f.clock.time = accepted.startsAt.plusSeconds(1)
        assertTrue(runCatching { f.records.addMemory(created.id, "기록", emptyList(), 5, true) }.isFailure)
        f.records.addMemory(created.id, "기록", listOf(RecordPhoto("photo", "content://photo")), 5, false)
        assertTrue(f.schedules.schedules.value.first { it.id == created.id }.completed)
        assertTrue(f.buckets.buckets.value.first { it.id == draft.bucketId }.completed)
        assertTrue(runCatching { f.records.addMemory(created.id, "중복", listOf(RecordPhoto("photo2", "content://photo2")), 5, true) }.isFailure)
        val memory = f.records.records.value.memories.first { it.entryId == created.id }
        assertTrue(runCatching { f.records.updateMemory(memory.copy(photos = emptyList())) }.isFailure)
        f.records.updateMemory(memory.copy(photos = listOf(RecordPhoto("replacement", "content://replacement"))))
        assertEquals("replacement", f.records.records.value.entries.first { it.id == created.id }.representativePhotoId)
        assertFalse(f.records.records.value.photos[created.id].orEmpty().any { it.id == "photo" })
        f.records.deleteMemory(memory.id)
        assertNull(f.records.records.value.entries.first { it.id == created.id }.representativePhotoId)
        assertTrue(f.schedules.schedules.value.first { it.id == created.id }.completed)
        assertTrue(runCatching { f.schedules.cancel(created.id, f.schedules.schedules.value.first { it.id == created.id }.revision) }.isFailure)
        f.records.refresh()
        assertEquals(CalendarEntryKind.MEMORY, f.records.records.value.entries.first { it.id == created.id }.kind)
    }
    @Test fun recordRegistrationUsesTheSharedScheduleAndKeepsTheLinkedBucket() = runTest {
        val f = Fixture()
        val draft = f.draft()
        f.records.refresh()
        val local = LocalDateTime.ofInstant(draft.startsAt, ZoneId.of("Asia/Seoul"))
        val request = ScheduleRequest(draft.bucketId, local.toLocalDate(), local.toLocalTime(), setOf("me", "mother"), "함께 가요", ScheduleRequestChannel.APP, "record-request")
        val first = f.records.requestSchedule(request)
        val retry = f.records.requestSchedule(request)
        assertEquals(first.id, retry.id)
        assertEquals(com.kiri.domain.home.ScheduleStatus.PENDING_RESPONSE, first.status)
        assertEquals(setOf(first.id), f.buckets.buckets.value.first { it.id == draft.bucketId }.linkedScheduleIds)
        f.user = "mother"
        val pending = f.schedules.schedules.value.first { it.id == first.id }
        f.schedules.respond(first.id, MemberResponse.ACCEPTED, pending.revision)
        f.records.refresh()
        assertEquals(com.kiri.domain.home.ScheduleStatus.CONFIRMED, f.records.records.value.entries.first { it.id == first.id }.status)
        f.user = "me"
        f.schedules.cancel(first.id, f.schedules.schedules.value.first { it.id == first.id }.revision)
        f.records.refresh()
        assertFalse(f.records.records.value.entries.any { it.id == first.id })
        assertTrue(f.buckets.buckets.value.first { it.id == draft.bucketId }.linkedScheduleIds.isEmpty())
    }
    @Test fun deletingTheLastReviewKeepsCompletionButClearsThePhotoGallery() = runTest {
        val f = Fixture()
        val draft = f.draft()
        val schedule = f.schedules.create(draft, "complete-and-delete")
        f.user = "mother"
        f.schedules.respond(schedule.id, MemberResponse.ACCEPTED, schedule.revision)
        f.user = "me"
        f.clock.time = schedule.startsAt.plusSeconds(1)
        f.records.refresh()
        f.records.addMemory(schedule.id, "즐거웠어요", listOf(RecordPhoto("one", "content://test/photo")), 5, true)
        val review = f.records.records.value.memories.single { it.entryId == schedule.id }
        f.records.deleteMemory(review.id)
        val entry = f.records.records.value.entries.single { it.id == schedule.id }
        assertEquals(CalendarEntryKind.MEMORY, entry.kind)
        assertTrue(entry.completed)
        assertNull(entry.representativePhotoId)
        assertTrue(f.records.records.value.photos[schedule.id].isNullOrEmpty())
        assertTrue(f.buckets.buckets.value.first { it.id == draft.bucketId }.completed)
    }
    @Test fun onlyOneScheduleCanTakeADayAndCancelledOnesFreeIt() = runTest {
        val f = Fixture(); val draft = f.draft()
        val first = f.schedules.create(draft, "first")
        val market = f.buckets.save("place-market")
        val sameDay = ScheduleDraft(market.id, market.missionId, draft.startsAt!!.plusSeconds(3600), setOf("mother"), "", "시장")
        // 같은 날 다른 시간이어도 새 일정을 만들 수 없다
        val error = runCatching { f.schedules.create(sameDay, "same-day") }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertEquals(ONE_SCHEDULE_PER_DAY_MESSAGE, error?.message)
        // 다른 날은 만들 수 있다
        val nextDay = f.schedules.create(sameDay.copy(startsAt = draft.startsAt!!.plusSeconds(86400)), "next-day")
        // 다른 일정이 있는 날로 옮길 수 없고, 자기 날짜 안에서 시간만 바꾸는 것은 된다
        assertTrue(runCatching { f.schedules.update(nextDay.id, sameDay, nextDay.revision) }.isFailure)
        val moved = f.schedules.update(nextDay.id, sameDay.copy(startsAt = nextDay.startsAt.plusSeconds(3600)), nextDay.revision)
        assertEquals(nextDay.startsAt.plusSeconds(3600), moved.startsAt)
        // 취소한 일정은 그날을 차지하지 않는다
        f.schedules.cancel(first.id, first.revision)
        val freed = f.schedules.update(moved.id, sameDay, moved.revision)
        assertEquals(sameDay.startsAt, freed.startsAt)
    }
}
