package com.kiri.feature.home.presentation

import com.kiri.domain.home.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun duplicateSubmissionIsIgnoredAndFailurePreservesDraftAndSavedAnswer() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onAction(HomeAction.EditAnswer)
        vm.onAction(HomeAction.ChangeAnswer("새 답변"))
        repo.failSave = true
        vm.onAction(HomeAction.SaveAnswer)
        vm.onAction(HomeAction.SaveAnswer)
        advanceUntilIdle()
        assertEquals(1, repo.saves)
        assertEquals("새 답변", vm.state.value.draft)
        assertEquals("기존 답변", vm.state.value.home!!.answers["me"])
        assertTrue(vm.state.value.canSave)
        assertNotNull(vm.state.value.error)
        repo.failSave = false
        vm.onAction(HomeAction.SaveAnswer)
        advanceUntilIdle()
        assertEquals("새 답변", vm.state.value.home!!.answers["me"])
        assertEquals("가족 답변", vm.state.value.home!!.answers["other"])
        assertTrue(vm.state.value.answerSaved)
        assertTrue(vm.state.value.editing)
    }

    @Test fun blankDraftNeverReachesRepository() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onAction(HomeAction.EditAnswer)
        vm.onAction(HomeAction.ChangeAnswer(" \n "))
        vm.onAction(HomeAction.SaveAnswer)
        advanceUntilIdle()
        assertEquals(0, repo.saves)
    }

    @Test fun flipAndBackPreserveDataAndUnsubmittedDraft() = runTest(dispatcher) {
        val vm = HomeViewModel(TestRepository())
        advanceUntilIdle()
        val before = vm.state.value.home
        vm.onAction(HomeAction.Flip)
        assertTrue(vm.state.value.showingMission)
        assertEquals(before, vm.state.value.home)
        vm.onAction(HomeAction.EditAnswer)
        vm.onAction(HomeAction.ChangeAnswer("아직 작성 중"))
        vm.onAction(HomeAction.Back)
        vm.onAction(HomeAction.EditAnswer)
        assertEquals("아직 작성 중", vm.state.value.draft)
    }

    @Test fun browsingBlocksFamilyInteractions() = runTest(dispatcher) {
        val repo = TestRepository()
        repo.home = repo.home.copy(familyName = null, question = null, members = emptyList(), answers = emptyMap())
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onAction(HomeAction.EditAnswer)
        vm.onAction(HomeAction.SelectMember("other"))
        vm.onAction(HomeAction.Flip)
        assertFalse(vm.state.value.editing)
        assertFalse(vm.state.value.showingMission)
        assertNull(vm.state.value.selectedMemberId)
    }

    @Test fun successfulPokeShowsMemberNotificationNotice() = runTest(dispatcher) {
        val repo = TestRepository().also {
            it.home = it.home.copy(
                members = it.home.members + FamilyMember("pending", "엄마", "엄마"),
                answers = mapOf("me" to "기존 답변"),
            )
        }
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onAction(HomeAction.SelectMember("pending"))
        vm.onAction(HomeAction.Poke)
        advanceUntilIdle()
        assertEquals("엄마를 콕 찔렀습니다!", vm.state.value.notice)
    }

    private class TestRepository : HomeRepository {
        var saves = 0
        var failSave = false
        var home = FamilyHome(LocalDate.of(2026, 10, 1), "me", "가족",
            listOf(FamilyMember("me", "나", "자녀"), FamilyMember("other", "가족", "기타")),
            DailyQuestion("q", LocalDate.of(2026, 10, 1), "질문"),
            mapOf("me" to "기존 답변", "other" to "가족 답변"), emptySet(), null)
        override suspend fun load() = home
        override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome {
            saves++
            delay(100)
            if (failSave) throw HomeException(HomeFailure.NETWORK)
            home = home.copy(answers = home.answers + ("me" to answer))
            return home
        }
        override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome {
            home = home.copy(pokedMemberIds = home.pokedMemberIds + memberId)
            return home
        }
    }
}
