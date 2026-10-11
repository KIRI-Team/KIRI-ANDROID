package com.kiri.feature.place.presentation

import com.kiri.feature.place.PlaceViewModel
import com.kiri.feature.place.model.*

import com.kiri.domain.place.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun latestSearchWinsAndServerCountIsNotPageSize() = runTest(dispatcher) {
        val repo = TestRepository(); val vm = PlaceViewModel(repo); advanceUntilIdle()
        vm.onEvent(PlaceUiEvent.SearchChanged("old")); vm.onEvent(PlaceUiEvent.SearchChanged("new")); advanceUntilIdle()
        assertEquals("new", repo.last.keyword)
        assertEquals(48, vm.uiState.value.totalCount)
        assertEquals(1, vm.uiState.value.items.size)
        vm.onEvent(PlaceUiEvent.CategorySelected(PlaceCategory.CULTURE)); advanceUntilIdle()
        assertEquals(PlaceCategory.CULTURE, repo.last.category)
    }
    @Test fun saveFailurePreservesDialogAndDuplicateSubmissionIsIgnored() = runTest(dispatcher) {
        val repo = TestRepository(); val vm = PlaceViewModel(repo); advanceUntilIdle()
        vm.onEvent(PlaceUiEvent.SaveClicked(repo.mission)); repo.failSave = true
        vm.onEvent(PlaceUiEvent.SaveConfirmed); vm.onEvent(PlaceUiEvent.SaveConfirmed); vm.onEvent(PlaceUiEvent.SaveDismissed); advanceUntilIdle()
        assertEquals(1, repo.saves); assertNotNull(vm.uiState.value.pending)
        assertFalse(vm.uiState.value.items.single().saved)
        assertNotNull(vm.uiState.value.error)
        repo.failSave = false; vm.onEvent(PlaceUiEvent.SaveConfirmed); advanceUntilIdle()
        assertNull(vm.uiState.value.pending); assertTrue(vm.uiState.value.toast)
        assertTrue(vm.uiState.value.items.single().saved)
        vm.onEvent(PlaceUiEvent.SavedClicked); advanceUntilIdle()
        assertEquals(1, vm.uiState.value.savedItems.size)
    }
    @Test fun paginationDeduplicatesAndCanRetry() = runTest(dispatcher) {
        val repo = TestRepository(); val vm = PlaceViewModel(repo); advanceUntilIdle()
        repo.failLoad = true; vm.onEvent(PlaceUiEvent.MoreRequested); vm.onEvent(PlaceUiEvent.MoreRequested); advanceUntilIdle()
        assertNotNull(vm.uiState.value.error); assertEquals(1, vm.uiState.value.items.size)
        repo.failLoad = false; vm.onEvent(PlaceUiEvent.MoreRequested); advanceUntilIdle()
        assertEquals(1, vm.uiState.value.items.size); assertNull(vm.uiState.value.nextCursor)
    }
    private class TestRepository : PlaceRepository {
        val mission = PlaceMission("one", "미션", "장소", "지역", PlaceCategory.FOOD)
        var last = PlaceQuery(); var failSave = false; var failLoad = false; var saves = 0
        override suspend fun load(query: PlaceQuery): PlacePage {
            last = query; delay(20); if (failLoad) error("load")
            return PlacePage(listOf(mission), 48, if (query.cursor == null) "next" else null)
        }
        override suspend fun save(missionId: String) { saves++; delay(20); if (failSave) error("save") }
        override suspend fun saved() = listOf(mission.copy(saved = true))
    }
}
