package com.kiri.feature.detail.presentation

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.detail.*
import com.kiri.feature.detail.DetailViewModel
import com.kiri.feature.detail.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryFlowTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private val valid = MemoryDraft(5, listOf("content://photo/1"), "가족과 즐거운 하루", false)

    @Test fun everyRequiredInputAndTextLimitAreEnforced() {
        assertTrue(valid.canSubmit)
        listOf(valid.copy(rating = 0), valid.copy(rating = 6), valid.copy(photos = emptyList()), valid.copy(text = "  \n "), valid.copy(text = "가".repeat(501)), valid.copy(repeatActivity = null)).forEach { assertFalse(it.canSubmit) }
        assertTrue(valid.copy(text = "가".repeat(500)).canSubmit)
    }
    @Test fun invalidSubmissionKeepsDraftAndValidSubmissionDoesNotWriteRepository() = runTest(dispatcher) {
        val repository = Repository()
        val vm = DetailViewModel(repository, SavedStateHandle(mapOf("missionId" to "demo")))
        advanceUntilIdle()
        vm.updateMemoryDraft(valid.copy(photos = emptyList()))
        assertFalse(vm.submitMemoryPreview())
        assertNull(vm.uiState.value.submittedMemory)
        assertEquals(valid.text, vm.uiState.value.memoryDraft.text)
        vm.updateMemoryDraft(valid)
        assertTrue(vm.submitMemoryPreview())
        assertEquals(valid, vm.uiState.value.submittedMemory)
        assertTrue(vm.uiState.value.memoriesSelected)
        assertEquals(0, repository.writes)
        assertFalse(vm.uiState.value.detail!!.completed)
        vm.updateMemoryDraft(valid.copy(text = "수정"))
        assertTrue(vm.submitMemoryPreview())
        assertEquals("수정", vm.uiState.value.submittedMemory!!.text)
    }
    @Test fun draftAndTabRestoreAndPhotosAreDeduplicated() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("missionId" to "demo"))
        val vm = DetailViewModel(Repository(), handle)
        advanceUntilIdle()
        vm.updateMemoryDraft(valid.copy(text = "가".repeat(501), photos = valid.photos + valid.photos))
        vm.selectMemories(true)
        val restored = DetailViewModel(Repository(), handle)
        advanceUntilIdle()
        assertTrue(restored.uiState.value.memoriesSelected)
        assertEquals(500, restored.uiState.value.memoryDraft.text.length)
        assertEquals(1, restored.uiState.value.memoryDraft.photos.size)
        assertFalse(restored.uiState.value.memoryDraft.repeatActivity!!)
        assertNull(restored.uiState.value.submittedMemory)
    }
    @Test fun recordNotificationOpensMemoriesButDoesNotOverrideLaterTabSelection() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("missionId" to "demo", "memories" to "true"))
        val vm = DetailViewModel(Repository(), handle)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.memoriesSelected)
        vm.selectMemories(false)
        val restored = DetailViewModel(Repository(), handle)
        advanceUntilIdle()
        assertFalse(restored.uiState.value.memoriesSelected)
    }
    private class Repository : DetailRepository {
        var writes = 0
        override suspend fun load(missionId: String) = figmaDetailSample().copy(id = missionId)
        override suspend fun applyConditions(missionId: String, conditions: Set<Condition>): MissionDetail { writes++; return load(missionId) }
    }
}
