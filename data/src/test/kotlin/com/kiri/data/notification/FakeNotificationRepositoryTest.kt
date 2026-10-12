package com.kiri.data.notification

import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.home.HomeScenario
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class FakeNotificationRepositoryTest {
    @Test fun readStateSurvivesReloadAndMissingTargetIsBlocked() = runTest {
        val repository = FakeNotificationRepository(FakeHomeRepository(latencyMillis = 0))
        val original = repository.load()
        assertTrue(original.zipWithNext().all { (a, b) -> a.createdAt >= b.createdAt })
        val unread = original.first { !it.read }
        assertTrue(repository.canOpen(unread))
        assertFalse(repository.canOpen(unread.copy(targetId = "missing")))
        repository.markRead(unread.id)
        assertTrue(repository.load().first { it.id == unread.id }.read)
        repository.markAllRead()
        val read = repository.load()
        assertEquals(original.size, read.size)
        assertTrue(read.all { it.read })
    }
    @Test fun browsingHasNoFamilyInbox() = runTest {
        val repository = FakeNotificationRepository(FakeHomeRepository(scenario = HomeScenario.BROWSING, latencyMillis = 0))
        assertTrue(repository.load().isEmpty())
    }
}
