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
