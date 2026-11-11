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
import kotlinx.coroutines.flow.first
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


    // 가족 정보만 돌려주는 홈 저장소. familyName 이 null 이면 둘러보기 사용자다
    private fun homeWith(familyName: String?) = object : com.kiri.domain.home.HomeRepository {
        private val family = com.kiri.domain.home.FamilyHome(java.time.LocalDate.of(2026, 11, 11), "13", familyName,
            emptyList(), null, emptyMap(), emptySet(), null, serverBacked = true)
        override suspend fun load() = family
        override suspend fun saveAnswer(question: com.kiri.domain.home.DailyQuestion, answer: String) = family
        override suspend fun poke(question: com.kiri.domain.home.DailyQuestion, memberId: String) = family
    }

    @Test fun browsingUserCannotSaveBucketAndSeesNotice() = runTest(dispatcher) {
        val buckets = Buckets(emptyList())
        val vm = MissionPreviewViewModel(Repository(), SavedStateHandle(mapOf("missionId" to "place-river")), buckets,
            home = homeWith(familyName = null))
        advanceUntilIdle()

        vm.onEvent(com.kiri.feature.detail.model.MissionPreviewUiEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(0, buckets.saves)
        assertFalse(vm.uiState.value.saving)
        assertEquals(com.kiri.feature.detail.model.MissionPreviewUiEffect.ShowMessage("가족에 참여하면 이용할 수 있어요."),
            vm.effect.first())
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
