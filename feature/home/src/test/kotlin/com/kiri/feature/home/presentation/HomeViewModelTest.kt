package com.kiri.feature.home.presentation

import com.kiri.feature.home.HomeViewModel
import com.kiri.feature.home.model.*

import com.kiri.domain.home.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Test fun resumeRefreshPreservesCachedContentAndAnUnsubmittedAnswer() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.AnswerEditClicked)
        vm.onEvent(HomeUiEvent.AnswerChanged("작성 중인 답변"))
        repo.home = repo.home.copy(schedules = listOf(HomeSchedule("confirmed", "확정된 미션",
            java.time.Instant.parse("2030-01-01T00:00:00Z"), ScheduleStatus.CONFIRMED, true)))
        vm.refreshOnResume()
        assertFalse(vm.uiState.value.loading)
        assertNotNull(vm.uiState.value.home)
        advanceUntilIdle()
        assertEquals("confirmed", vm.uiState.value.home!!.schedules.single().id)
        assertEquals("작성 중인 답변", vm.uiState.value.draft)
        assertTrue(vm.uiState.value.editing)
    }
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun invitationUsesOnboardingCodeAndIsBufferedBeforeCollection() = runTest(dispatcher) {
        val repo = TestRepository().apply { home = home.copy(inviteCode = "TESTCODE") }
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.InviteFamilyClicked)
        assertEquals(HomeUiEffect.InviteFamily(com.kiri.feature.onboarding.TemporaryFamilyCode.code), vm.effect.first())
        assertEquals(com.kiri.feature.onboarding.TemporaryFamilyCode.code, vm.uiState.value.home?.inviteCode)
    }

    @Test fun duplicateSubmissionIsIgnoredAndFailurePreservesDraftAndSavedAnswer() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.AnswerEditClicked)
        vm.onEvent(HomeUiEvent.AnswerChanged("새 답변"))
        repo.failSave = true
        vm.onEvent(HomeUiEvent.AnswerSaveClicked)
        vm.onEvent(HomeUiEvent.AnswerSaveClicked)
        advanceUntilIdle()
        assertEquals(1, repo.saves)
        assertEquals("새 답변", vm.uiState.value.draft)
        assertEquals("기존 답변", vm.uiState.value.home!!.answers["me"])
        assertTrue(vm.uiState.value.canSave)
        assertNotNull(vm.uiState.value.error)
        repo.failSave = false
        vm.onEvent(HomeUiEvent.AnswerSaveClicked)
        advanceUntilIdle()
        assertEquals("새 답변", vm.uiState.value.home!!.answers["me"])
        assertEquals("가족 답변", vm.uiState.value.home!!.answers["other"])
        assertTrue(vm.uiState.value.answerSaved)
        assertTrue(vm.uiState.value.editing)
    }

    @Test fun blankDraftNeverReachesRepository() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.AnswerEditClicked)
        vm.onEvent(HomeUiEvent.AnswerChanged(" \n "))
        vm.onEvent(HomeUiEvent.AnswerSaveClicked)
        advanceUntilIdle()
        assertEquals(0, repo.saves)
    }

    @Test fun flipAndBackPreserveDataAndUnsubmittedDraft() = runTest(dispatcher) {
        val vm = HomeViewModel(TestRepository())
        advanceUntilIdle()
        val before = vm.uiState.value.home
        vm.onEvent(HomeUiEvent.CardFlipped)
        assertTrue(vm.uiState.value.showingMission)
        assertEquals(before, vm.uiState.value.home)
        vm.onEvent(HomeUiEvent.AnswerEditClicked)
        vm.onEvent(HomeUiEvent.AnswerChanged("아직 작성 중"))
        vm.onEvent(HomeUiEvent.BackClicked)
        vm.onEvent(HomeUiEvent.AnswerEditClicked)
        assertEquals("아직 작성 중", vm.uiState.value.draft)
    }

    @Test fun browsingBlocksFamilyInteractions() = runTest(dispatcher) {
        val repo = TestRepository()
        repo.home = repo.home.copy(familyName = null, question = null, members = emptyList(), answers = emptyMap())
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.AnswerEditClicked)
        vm.onEvent(HomeUiEvent.MemberSelected("other"))
        vm.onEvent(HomeUiEvent.CardFlipped)
        assertFalse(vm.uiState.value.editing)
        assertFalse(vm.uiState.value.showingMission)
        assertNull(vm.uiState.value.selectedMemberId)
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
        vm.onEvent(HomeUiEvent.MemberSelected("pending"))
        vm.onEvent(HomeUiEvent.PokeClicked)
        advanceUntilIdle()
        assertEquals("엄마를 콕 찔렀습니다!", vm.uiState.value.notice)
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
