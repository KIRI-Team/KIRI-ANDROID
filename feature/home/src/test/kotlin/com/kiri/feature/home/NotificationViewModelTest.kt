package com.kiri.feature.home

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.notification.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun openingInboxDoesNotReadAndFiltersKeepTotalUnreadCount() = runTest(dispatcher) {
        val repo = Repository()
        val handle = SavedStateHandle()
        val vm = NotificationViewModel(repo, handle)
        advanceUntilIdle()
        assertEquals("new", vm.uiState.value.items.first().id)
        assertEquals(2, vm.uiState.value.unreadCount)
        assertEquals(0, repo.reads)
        vm.select(NotificationCategory.RECORD)
        assertEquals(1, vm.uiState.value.visibleItems.size)
        assertEquals(2, vm.uiState.value.unreadCount)
        val restored = NotificationViewModel(repo, handle)
        advanceUntilIdle()
        assertEquals(NotificationCategory.RECORD, restored.uiState.value.selected)
        vm.readAll(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.unreadCount)
        assertEquals(2, vm.uiState.value.items.size)
    }
    @Test fun inaccessibleTargetReadsWithoutNavigationAndFailureCanRetry() = runTest(dispatcher) {
        val repo = Repository().apply { fail = true }
        val vm = NotificationViewModel(repo, SavedStateHandle())
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        repo.fail = false; vm.reload(); advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        vm.open(vm.uiState.value.items.first().copy(targetAccessible = false)); advanceUntilIdle()
        assertEquals("더 이상 확인할 수 없는 내용이에요", vm.uiState.value.notice)
        assertTrue(vm.uiState.value.items.first().read)
        vm.open(vm.uiState.value.items.last()); advanceUntilIdle()
        assertEquals("old", vm.target.first().id)
    }
    @Test fun failedReadAllPreservesUnreadItemsForRetry() = runTest(dispatcher) {
        val repo = Repository()
        val vm = NotificationViewModel(repo, SavedStateHandle()); advanceUntilIdle()
        repo.fail = true; vm.readAll(); advanceUntilIdle()
        assertEquals(2, vm.uiState.value.unreadCount)
        assertNotNull(vm.uiState.value.notice)
        repo.fail = false; vm.readAll(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.unreadCount)
    }
    private class Repository : NotificationRepository {
        var fail = false
        var reads = 0
        val items = listOf(
            FamilyNotification("old", "가족", "기록", "내용", Instant.EPOCH, NotificationCategory.RECORD, NotificationTarget.MEMORY, "mission"),
            FamilyNotification("new", "가족", "일정", "내용", Instant.EPOCH.plusSeconds(1), NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE, "schedule"),
        )
        override suspend fun load(): List<FamilyNotification> { if (fail) error("network"); return items }
        override suspend fun markRead(id: String) { if (fail) error("network"); reads++ }
        override suspend fun markAllRead() { if (fail) error("network"); reads++ }
    }
}
