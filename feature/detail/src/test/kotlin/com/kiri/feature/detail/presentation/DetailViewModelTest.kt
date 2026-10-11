package com.kiri.feature.detail.presentation

import com.kiri.feature.detail.DetailViewModel
import com.kiri.feature.detail.model.*

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }
    private fun vm(repo: TestRepository, handle: SavedStateHandle = SavedStateHandle(mapOf("missionId" to "mission-42"))) = DetailViewModel(repo, handle)

    @Test fun failedApplyDoesNotNavigateAndSuccessfulApplyBuffersBackEffect() = runTest(dispatcher) {
        val repo = TestRepository().apply { failSave = true }
        val vm = vm(repo)
        advanceUntilIdle()
        vm.onEvent(DetailUiEvent.ApplyClicked)
        advanceUntilIdle()
        assertNull(withTimeoutOrNull(1) { vm.effect.first() })
        repo.failSave = false
        vm.onEvent(DetailUiEvent.ApplyClicked)
        advanceUntilIdle()
        assertEquals(DetailUiEffect.NavigateBack, vm.effect.first())
    }

    @Test fun loadsRequestedIdAndRetriesFailure() = runTest(dispatcher) {
        val repo = TestRepository().apply { failLoad = true }
        val vm = vm(repo)
        advanceUntilIdle()
        assertEquals("mission-42", repo.loadedId)
        assertNotNull(vm.uiState.value.error)
        repo.failLoad = false; vm.onEvent(DetailUiEvent.RetryClicked); advanceUntilIdle()
        assertEquals("원래 미션", vm.uiState.value.detail?.title)
        assertNull(vm.uiState.value.error)
    }
    @Test fun cancelDiscardsDraftWithoutChangingSavedConditions() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = vm(repo); advanceUntilIdle(); vm.onEvent(DetailUiEvent.CustomClicked)
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LESS_WALKING)); vm.onEvent(DetailUiEvent.CustomCancelled)
        assertTrue(vm.uiState.value.draft.isEmpty())
        assertEquals(0, repo.saves)
    }
    @Test fun failedApplyKeepsSelectionAndPreventsDuplicateSubmission() = runTest(dispatcher) {
        val repo = TestRepository().apply { failSave = true }
        val vm = vm(repo); advanceUntilIdle(); vm.onEvent(DetailUiEvent.CustomClicked); vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))
        vm.onEvent(DetailUiEvent.ApplyClicked); vm.onEvent(DetailUiEvent.ApplyClicked); vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LOWER_BUDGET)); advanceUntilIdle()
        assertEquals(1, repo.saves)
        assertEquals(setOf(Condition.MORE_REST), vm.uiState.value.draft)
        assertNotNull(vm.uiState.value.error)
        repo.failSave = false; vm.onEvent(DetailUiEvent.ApplyClicked); advanceUntilIdle()
        assertFalse(vm.uiState.value.saving)
        assertEquals("원래 미션", vm.uiState.value.detail?.title)
        assertEquals(8000, vm.uiState.value.detail?.expectedSteps)
        assertEquals("near", vm.uiState.value.nearby.first().id)
        vm.onEvent(DetailUiEvent.CustomClicked)
        assertEquals(setOf(Condition.MORE_REST), vm.uiState.value.draft)
    }
    @Test fun togglingAllOffClearsPreviouslyAppliedConditions() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = vm(repo); advanceUntilIdle(); vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LOWER_BUDGET)); vm.onEvent(DetailUiEvent.ApplyClicked); advanceUntilIdle()
        vm.onEvent(DetailUiEvent.CustomClicked); vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LOWER_BUDGET)); vm.onEvent(DetailUiEvent.ApplyClicked); advanceUntilIdle()
        assertTrue(repo.detail.conditions.isEmpty())
    }
    @Test fun draftSurvivesSavedStateRecreation() = runTest(dispatcher) {
        val repo = TestRepository()
        val handle = SavedStateHandle(mapOf("missionId" to "mission-42"))
        val first = vm(repo, handle); advanceUntilIdle(); first.onEvent(DetailUiEvent.ConditionToggled(Condition.AVOID_CROWDS))
        val restored = vm(repo, SavedStateHandle(mapOf("missionId" to "mission-42", "conditionDraft" to handle.get<ArrayList<String>>("conditionDraft"))))
        advanceUntilIdle()
        assertEquals(first.uiState.value.draft, restored.uiState.value.draft)
    }
    @Test fun completedMissionRejectsCustomization() = runTest(dispatcher) {
        val repo = TestRepository().apply { detail = detail.copy(completed = true) }
        val vm = vm(repo); advanceUntilIdle(); vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LESS_WALKING)); vm.onEvent(DetailUiEvent.ApplyClicked); advanceUntilIdle()
        assertTrue(vm.uiState.value.draft.isEmpty()); assertEquals(0, repo.saves)
    }

    private class TestRepository : DetailRepository {
        var detail = MissionDetail("mission-42", "원래 미션", "가족", null, null, "장소", 8000, 480, emptyList(), false, emptyList(), emptyList(), listOf(
            NearbyPlace("far", "먼 곳", "", "", 900, false, 0), NearbyPlace("near", "가까운 쉼터", "", "", 100, true, 3000),
        ))
        var failLoad = false; var failSave = false; var saves = 0; var loadedId = ""
        override suspend fun load(missionId: String): MissionDetail { loadedId = missionId; if (failLoad) error("load"); return detail }
        override suspend fun applyConditions(missionId: String, conditions: Set<Condition>): MissionDetail {
            saves++; delay(100); if (failSave) error("save")
            detail = detail.copy(conditions = conditions); return detail
        }
    }
}
