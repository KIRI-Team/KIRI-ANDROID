package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.FakeRecordRepository
import com.kiri.domain.record.RecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecordDetailViewModelTest {
    @Test
    fun editingCannotSaveAfterRemovingTheLastPhoto() = runTest {
        val repo = repository()
        val vm = RecordDetailViewModel(repo, handle())
        val original = vm.uiState.value.memories.single()
        vm.editMemory(original.id)
        vm.updateDraft(vm.uiState.value.memoryDraft.copy(photos = emptyList(), text = "사진 없는 수정"))
        vm.saveMemory()
        assertFalse(vm.uiState.value.memoryDraft.canSubmit)
        assertEquals(original, repo.records.value.memories.find { it.id == original.id })
        assertFalse(vm.uiState.value.detail.saving)
    }

    @Test
    fun customAccessUsesInjectedClockInsteadOfDeviceDate() = runTest {
        val clock =
            java.time.Clock.fixed(
                java.time.Instant.parse("2026-10-13T12:00:00Z"),
                java.time.ZoneOffset.UTC,
            )
        val repo =
            FakeRecordRepository(
                FakeHomeRepository(clock, latencyMillis = 0),
                FakeMissionRepository(),
                clock = clock,
            )
        val vm = RecordDetailViewModel(repo, handle("schedule-demo"), clock)
        vm.onDetailEvent(com.kiri.feature.detail.model.DetailUiEvent.CustomClicked)
        assertEquals(java.time.LocalDate.of(2026, 10, 13), vm.uiState.value.today)
        assertEquals(RecordDetailEffect.Custom, vm.effect.first())
    }

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun repository() =
        FakeRecordRepository(FakeHomeRepository(latencyMillis = 0), FakeMissionRepository())

    private fun handle(id: String = "record-memory-1") = SavedStateHandle(mapOf("entryId" to id))

    @Test
    fun photoSaveUpdatesCalendarSourceAndEmitsBackOnlyAfterSuccess() = runTest {
        val repo = repository()
        val calendar = RecordViewModel(SavedStateHandle(), repo)
        val detail = RecordDetailViewModel(repo, handle())
        detail.beginPhotoSelection()
        detail.selectPhoto("pixel-2")
        assertEquals("group-1", detail.uiState.value.selectedPhotoId)
        detail.selectPhoto("pixel-1")
        detail.savePhoto()
        assertEquals(RecordDetailEffect.ReturnToDetail, detail.effect.first())
        assertEquals("pixel-1", detail.uiState.value.representativePhoto?.id)
        val date = detail.uiState.value.entry!!.date
        assertEquals("pixel-1", calendar.uiState.value.calendarPhotos[date]?.id)
        assertEquals("사진 변경이 완료되었습니다!", detail.uiState.value.toast)
    }

    @Test
    fun failedPhotoSaveKeepsSelectionAndRetriesWithoutAFalseSuccessToast() = runTest {
        val backing = repository()
        var fail = true
        val repo =
            object : RecordRepository by backing {
                override suspend fun changeRepresentativePhoto(entryId: String, photoId: String) {
                    if (fail) error("network")
                    backing.changeRepresentativePhoto(entryId, photoId)
                }
            }
        val vm = RecordDetailViewModel(repo, handle())
        vm.selectPhoto("pixel-1")
        vm.savePhoto()
        assertNotNull(vm.uiState.value.detail.error)
        assertEquals("pixel-1", vm.uiState.value.selectedPhotoId)
        assertEquals("group-1", vm.uiState.value.representativePhoto?.id)
        assertNull(vm.uiState.value.toast)
        fail = false
        vm.savePhoto()
        assertNull(vm.uiState.value.detail.error)
        assertEquals(RecordDetailEffect.ReturnToDetail, vm.effect.first())
    }

    @Test
    fun editDraftRestoresAndSavingUpdatesOnlyTheSelectedMemory() = runTest {
        val repo = repository()
        val handle = handle()
        val vm = RecordDetailViewModel(repo, handle)
        val memory = vm.uiState.value.memories.single()
        vm.editMemory(memory.id)
        assertEquals(RecordDetailEffect.EditMemory, vm.effect.first())
        vm.updateDraft(vm.uiState.value.memoryDraft.copy(text = "새로운 가족 기록"))
        val restored = RecordDetailViewModel(repo, handle)
        assertEquals("새로운 가족 기록", restored.uiState.value.memoryDraft.text)
        restored.saveMemory()
        assertEquals(RecordDetailEffect.ReturnToDetail, restored.effect.first())
        assertEquals("새로운 가족 기록", restored.uiState.value.memories.single().text)
        assertEquals(2, repo.records.value.memories.size)
        restored.deleteMemory(memory.id)
        assertTrue(restored.uiState.value.memories.isEmpty())
        assertEquals(1, repo.records.value.memories.size)
    }
}
