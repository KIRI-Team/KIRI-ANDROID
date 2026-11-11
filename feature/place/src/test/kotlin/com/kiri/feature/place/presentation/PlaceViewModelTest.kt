package com.kiri.feature.place.presentation

import com.kiri.feature.place.PlaceViewModel
import com.kiri.feature.place.model.*

import com.kiri.domain.place.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.first
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceViewModelTest {
    @Test fun returningFromCompletionRefreshesBothCardsAndSavedListBeforeResave() = runTest(dispatcher) {
        var mission = PlaceMission("repeat", "활동", "장소", "서울", PlaceCategory.OUTDOOR, saved = true)
        val repository = object : PlaceRepository {
            override suspend fun load(query: PlaceQuery) = PlacePage(listOf(mission), 1)
            override suspend fun saved() = listOf(mission)
            override suspend fun save(missionId: String) { mission = mission.copy(saved = true) }
        }
        val vm = PlaceViewModel(repository)
        advanceUntilIdle()
        vm.onEvent(PlaceUiEvent.SavedClicked)
        advanceUntilIdle()
        mission = mission.copy(saved = false, previouslyCompleted = true)
        vm.refreshSaves()
        advanceUntilIdle()
        assertEquals("다시 버킷에 저장", vm.uiState.value.items.single().saveLabel)
        assertEquals("다시 버킷에 저장", vm.uiState.value.savedItems.single().saveLabel)
        vm.onEvent(PlaceUiEvent.SaveClicked(vm.uiState.value.savedItems.single()))
        vm.onEvent(PlaceUiEvent.SaveConfirmed)
        advanceUntilIdle()
        assertEquals("저장됨", vm.uiState.value.savedItems.single().saveLabel)
        assertEquals("저장됨", vm.uiState.value.items.single().saveLabel)
    }
    @Test fun applyingFiltersReloadsAndRestoresTheAppliedQuery() = runTest(dispatcher) {
        val handle = androidx.lifecycle.SavedStateHandle()
        val repo = TestRepository()
        val vm = PlaceViewModel(repo, handle)
        advanceUntilIdle()
        val filters = PlaceFilters(setOf("서울", "인천"), 3, 20000, 570, 3, 1000, 5000)
        vm.onEvent(PlaceUiEvent.FiltersApplied(filters)); advanceUntilIdle()
        assertEquals(filters, repo.last.filters)
        assertNull(repo.last.cursor)
        val restored = PlaceViewModel(repo, handle); advanceUntilIdle()
        assertEquals(filters, restored.uiState.value.query.filters)
    }
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

    // 가족 정보만 돌려주는 홈 저장소. familyName 이 null 이면 둘러보기 사용자다
    private fun homeWith(familyName: String?) = object : com.kiri.domain.home.HomeRepository {
        private val family = com.kiri.domain.home.FamilyHome(java.time.LocalDate.of(2026, 11, 11), "13", familyName,
            emptyList(), null, emptyMap(), emptySet(), null, serverBacked = true)
        override suspend fun load() = family
        override suspend fun saveAnswer(question: com.kiri.domain.home.DailyQuestion, answer: String) = family
        override suspend fun poke(question: com.kiri.domain.home.DailyQuestion, memberId: String) = family
    }

    @Test fun browsingUserCannotOpenBucketSaveAndSeesNotice() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = PlaceViewModel(repo, home = homeWith(familyName = null)); advanceUntilIdle()

        vm.onEvent(PlaceUiEvent.SaveClicked(repo.mission)); advanceUntilIdle()

        // 저장 확인을 띄우지 않고 안내한다
        assertNull(vm.uiState.value.pending)
        assertEquals(PlaceUiEffect.ShowMessage("가족에 참여하면 이용할 수 있어요."), vm.effect.first())
        assertEquals(0, repo.saves)
    }

    @Test fun familyMemberOpensBucketSaveAsBefore() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = PlaceViewModel(repo, home = homeWith(familyName = "끼리네")); advanceUntilIdle()

        vm.onEvent(PlaceUiEvent.SaveClicked(repo.mission)); advanceUntilIdle()

        assertEquals(repo.mission, vm.uiState.value.pending)
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
