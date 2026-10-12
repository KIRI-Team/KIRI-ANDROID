package com.kiri.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.home.*
import com.kiri.feature.home.HomeViewModel
import com.kiri.feature.home.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeTutorialViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun firstVisitRunsThreeStepsOnceAndDoesNotMutateHomeData() = runTest(dispatcher) {
        val visits = Visits()
        val repository = Repository()
        val vm = HomeViewModel(repository, tutorialRepository = visits)
        advanceUntilIdle()
        val original = vm.uiState.value.home
        assertEquals(HomeTutorialStep.INITIAL, vm.uiState.value.tutorialStep)
        vm.onEvent(HomeUiEvent.TutorialNext)
        assertEquals(HomeTutorialStep.MISSION, vm.uiState.value.tutorialStep)
        assertTrue(vm.uiState.value.showingMission)
        vm.onEvent(HomeUiEvent.TutorialNext)
        assertEquals(HomeTutorialStep.RECOMMENDATIONS, vm.uiState.value.tutorialStep)
        vm.onEvent(HomeUiEvent.TutorialNext)
        assertNull(vm.uiState.value.tutorialStep)
        vm.onEvent(HomeUiEvent.RetryClicked)
        advanceUntilIdle()
        assertNull(vm.uiState.value.tutorialStep)
        assertEquals(original, vm.uiState.value.home)
        val recreated = HomeViewModel(repository, tutorialRepository = visits)
        advanceUntilIdle()
        assertNull(recreated.uiState.value.tutorialStep)
    }

    @Test fun activeStepAndCardSideRestoreButDismissedTutorialDoesNotReplay() = runTest(dispatcher) {
        val visits = Visits()
        val handle = SavedStateHandle()
        val vm = HomeViewModel(Repository(), tutorialRepository = visits, savedState = handle)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.TutorialNext)
        val restored = HomeViewModel(Repository(), tutorialRepository = visits, savedState = handle)
        advanceUntilIdle()
        assertEquals(HomeTutorialStep.MISSION, restored.uiState.value.tutorialStep)
        assertTrue(restored.uiState.value.showingMission)
        restored.onEvent(HomeUiEvent.TutorialDismissed)
        val next = HomeViewModel(Repository(), tutorialRepository = visits, savedState = handle)
        advanceUntilIdle()
        assertNull(next.uiState.value.tutorialStep)
        assertEquals(1, visits.claims)
    }

    @Test fun browsingAndLoadFailureDoNotConsumeFirstFamilyVisit() = runTest(dispatcher) {
        val visits = Visits()
        val repository = Repository().apply { fail = true }
        val vm = HomeViewModel(repository, tutorialRepository = visits)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadError)
        assertEquals(0, visits.claims)
        repository.fail = false
        repository.home = repository.home.copy(familyName = null)
        vm.onEvent(HomeUiEvent.RetryClicked); advanceUntilIdle()
        assertEquals(0, visits.claims)
        repository.home = repository.home.copy(familyName = "가족")
        vm.onEvent(HomeUiEvent.RetryClicked); advanceUntilIdle()
        assertEquals(HomeTutorialStep.INITIAL, vm.uiState.value.tutorialStep)
        assertEquals(1, visits.claims)
    }

    @Test fun emptyRecommendationListNeverLeavesABlankGuide() = runTest(dispatcher) {
        val vm = HomeViewModel(Repository().apply { home = home.copy(recommendations = emptyList()) }, tutorialRepository = Visits())
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.TutorialNext)
        vm.onEvent(HomeUiEvent.TutorialNext)
        assertNull(vm.uiState.value.tutorialStep)
    }

    private class Visits : HomeTutorialRepository {
        var claims = 0
        private val seen = mutableSetOf<String>()
        override suspend fun claimFirstVisit(userId: String): Boolean { claims++; return seen.add(userId) }
    }
    private class Repository : HomeRepository {
        var fail = false
        var home = FamilyHome(LocalDate.now(), "me", "가족", listOf(FamilyMember("me", "나", "아들")), DailyQuestion("q", LocalDate.now(), "오늘의 질문"), emptyMap(), emptySet(), WeeklyMission("m", "산책", "공원", emptyList(), false), recommendations = listOf(HomeRecommendation("r", "추천", "서울")))
        override suspend fun load(): FamilyHome { if (fail) error("offline"); return home }
        override suspend fun saveAnswer(question: DailyQuestion, answer: String) = home
        override suspend fun poke(question: DailyQuestion, memberId: String) = home
    }
}
