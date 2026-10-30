package com.kiri.feature.detail.presentation

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.detail.*
import com.kiri.feature.detail.MissionPreviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MissionPreviewViewModelTest {
    @Test fun completedHistoryAllowsOneNewSaveAndUnfinishedSaveTakesPriority() = runTest(dispatcher) {
        val bucket = com.kiri.domain.record.FamilyBucket("old", "place-river", "me",
            java.time.Instant.EPOCH, completed = true)
        val buckets = Buckets(listOf(bucket))
        val vm = MissionPreviewViewModel(Repository(), SavedStateHandle(mapOf("missionId" to "place-river")), buckets)
        advanceUntilIdle()
        assertEquals("다시 버킷에 저장", vm.uiState.value.saveLabel)
        vm.onEvent(com.kiri.feature.detail.model.MissionPreviewUiEvent.SaveClicked)
        vm.onEvent(com.kiri.feature.detail.model.MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals(1, buckets.saves)
        assertEquals("저장됨", vm.uiState.value.saveLabel)
        assertEquals(bucket, buckets.buckets.value.first())
        vm.retry()
        advanceUntilIdle()
        assertEquals("저장됨", vm.uiState.value.saveLabel)
        vm.onEvent(com.kiri.feature.detail.model.MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals(1, buckets.saves)
    }

    @Test fun failedResaveKeepsRepeatActionAvailableForRetry() = runTest(dispatcher) {
        val buckets = Buckets(listOf(com.kiri.domain.record.FamilyBucket("old", "place-river", "me",
            java.time.Instant.EPOCH, completed = true))).apply { fail = true }
        val vm = MissionPreviewViewModel(Repository(), SavedStateHandle(mapOf("missionId" to "place-river")), buckets)
        advanceUntilIdle()
        vm.onEvent(com.kiri.feature.detail.model.MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals("다시 버킷에 저장", vm.uiState.value.saveLabel)
        assertNotNull(vm.uiState.value.error)
        buckets.fail = false
        vm.onEvent(com.kiri.feature.detail.model.MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals("저장됨", vm.uiState.value.saveLabel)
    }

    private class Buckets(initial: List<com.kiri.domain.record.FamilyBucket>) : com.kiri.domain.record.BucketRepository {
        override val buckets = kotlinx.coroutines.flow.MutableStateFlow(initial)
        var saves = 0
        var fail = false
        override suspend fun refresh() {}
        override suspend fun mine() = buckets.value
        override suspend fun save(missionId: String): com.kiri.domain.record.FamilyBucket {
            if (fail) error("save")
            saves++
            return com.kiri.domain.record.FamilyBucket("new", missionId, "me", java.time.Instant.EPOCH)
                .also { buckets.value += it }
        }
        override suspend fun delete(id: String) = error("unused")
        override suspend fun link(id: String, scheduleId: String) = error("unused")
        override suspend fun unlink(id: String, scheduleId: String) = error("unused")
        override suspend fun complete(id: String) = error("unused")
    }

    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun defaultsToActivitiesAndRestoresReviewTab() = runTest(dispatcher) {
        val savedState = SavedStateHandle(mapOf("missionId" to "place-river"))
        val repo = Repository()
        val viewModel = MissionPreviewViewModel(repo, savedState)
        advanceUntilIdle()
        assertEquals("place-river", repo.id)
        assertFalse(viewModel.uiState.value.reviewsSelected)
        viewModel.selectReviews(true)
        val restored = MissionPreviewViewModel(repo, savedState)
        advanceUntilIdle()
        assertTrue(restored.uiState.value.reviewsSelected)
        restored.selectReviews(false)
        assertFalse(restored.uiState.value.reviewsSelected)
    }

    @Test fun failedLoadCanRetryWithoutLosingSelectedTab() = runTest(dispatcher) {
        val repo = Repository().apply { failing = true }
        val viewModel = MissionPreviewViewModel(repo, SavedStateHandle(mapOf("missionId" to "place-river")))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        viewModel.selectReviews(true)
        repo.failing = false
        viewModel.retry()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.preview)
        assertTrue(viewModel.uiState.value.reviewsSelected)
    }

    private class Repository : DetailRepository {
        var id = ""
        var failing = false
        override suspend fun load(missionId: String): MissionDetail {
            id = missionId
            if (failing) error("network")
            return figmaDetailSample().copy(id = missionId)
        }
        override suspend fun applyConditions(missionId: String, conditions: Set<Condition>) = load(missionId)
    }
}
