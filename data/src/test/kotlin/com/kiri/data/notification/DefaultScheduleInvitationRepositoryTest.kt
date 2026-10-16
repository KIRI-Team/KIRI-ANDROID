package com.kiri.data.notification

import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.domain.home.*
import com.kiri.domain.notification.*
import java.time.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DefaultScheduleInvitationRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-16T03:00:00Z"), ZoneOffset.UTC)
    @Test fun newAndChangedInvitationsUseCanonicalContentAndCurrentRevisionResponses() = runTest {
        val home = FakeHomeRepository(clock, latencyMillis = 0)
        val notifications = FakeNotificationRepository(home, clock)
        val missions = FakeMissionRepository()
        val repo = DefaultScheduleInvitationRepository(notifications, home, missions)
        val invite = repo.load("demo-invite")
        val changed = repo.load("demo-change")
        assertEquals(InvitationKind.NEW, invite.kind)
        assertEquals(InvitationKind.CHANGED, changed.kind)
        assertEquals(missions.load(invite.schedule.missionId), invite.mission)
        assertTrue("mother" in invite.schedule.acceptedMemberIds)
        assertFalse("mother" in changed.schedule.acceptedMemberIds)
        assertFalse(changed.accepted)
    }
    @Test fun concurrentConfirmationsUpdateExistingScheduleWithoutDuplicatesOrOtherScheduleChanges() = runTest {
        val home = FakeHomeRepository(clock, latencyMillis = 0)
        val repo = DefaultScheduleInvitationRepository(FakeNotificationRepository(home, clock), home, FakeMissionRepository())
        val invite = repo.load("demo-change")
        val before = home.load()
        List(4) { async { repo.confirm("demo-change") } }.awaitAll()
        val after = home.load()
        assertEquals(before.schedules.size, after.schedules.size)
        assertEquals(before.schedules.filter { it.id != invite.schedule.id }, after.schedules.filter { it.id != invite.schedule.id })
        assertTrue(repo.load("demo-change").accepted)
        assertEquals(setOf("daughter", "me"), repo.load("demo-change").schedule.acceptedMemberIds)
    }
    @Test fun staleDatesAndInaccessibleNotificationsCannotConfirm() = runTest {
        val home = FakeHomeRepository(clock, latencyMillis = 0)
        val source = FakeNotificationRepository(home, clock)
        val original = source.load()
        val stale = object : NotificationRepository by source {
            override suspend fun load() = original.map { it.copy(scheduleStartsAt = it.scheduleStartsAt?.minusSeconds(3600)) }
        }
        val before = home.load()
        val repo = DefaultScheduleInvitationRepository(stale, home, FakeMissionRepository())
        try { repo.confirm("demo-change"); fail("Stale invitation accepted") } catch (e: HomeException) { assertEquals(HomeFailure.UNAVAILABLE, e.reason) }
        assertEquals(before, home.load())
        val invite = original.first { it.id == "demo-invite" }
        try { home.confirmSchedule(invite.targetId, invite.scheduleStartsAt!!.minusSeconds(1)); fail("Old revision accepted") }
        catch (e: HomeException) { assertEquals(HomeFailure.UNAVAILABLE, e.reason) }
        assertEquals(before, home.load())
    }
}
