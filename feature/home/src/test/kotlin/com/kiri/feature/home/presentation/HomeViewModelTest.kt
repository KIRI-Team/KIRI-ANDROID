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
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun serverInviteOpensApiBackedFlowWithoutCachedCode() = runTest(dispatcher) {
        val repo = TestRepository().apply { home = home.copy(serverBacked = true) }
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.InviteFamilyClicked)
        assertEquals(HomeUiEffect.Navigate(HomeUiDestination.InviteFamily), vm.effect.first())
    }

    @Test fun tasteBannerRefreshesAfterSaveAndIsHiddenOnReadFailure() = runTest(dispatcher) {
        val tastes = TestTastes()
        val vm = HomeViewModel(TestRepository(), tasteRepository = tastes)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showTasteBanner)
        vm.onEvent(HomeUiEvent.DestinationClicked(HomeUiDestination.TasteSettings))
        assertEquals(HomeUiEffect.Navigate(HomeUiDestination.TasteSettings), vm.effect.first())
        tastes.result = Result.success(com.kiri.domain.model.taste.MyTaste("나",
            com.kiri.domain.model.taste.Taste(setOf(com.kiri.domain.model.taste.TasteCategory.OUTDOOR), com.kiri.domain.model.taste.TasteActivity.LIGHT)))
        vm.refreshOnResume(); advanceUntilIdle()
        assertFalse(vm.uiState.value.showTasteBanner)
        tastes.result = Result.failure(IllegalStateException("network"))
        vm.refreshOnResume(); advanceUntilIdle()
        assertFalse(vm.uiState.value.showTasteBanner)
        assertFalse(vm.uiState.value.loadError)
    }

    @Test fun serverCompletionFlagWinsEvenWithUnknownTasteFields() = runTest(dispatcher) {
        val tastes = TestTastes().apply {
            result = Result.success(com.kiri.domain.model.taste.MyTaste("나", null, isSet = true))
        }
        val vm = HomeViewModel(TestRepository(), tasteRepository = tastes)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showTasteBanner)
    }

    @Test fun browsingSkipsTasteQuery() = runTest(dispatcher) {
        val tastes = TestTastes()
        val repo = TestRepository().apply { home = home.copy(familyName = null) }
        val vm = HomeViewModel(repo, tasteRepository = tastes)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showTasteBanner)
        assertEquals(0, tastes.calls)
    }

    private class TestTastes : com.kiri.domain.repository.TasteRepository {
        var calls = 0
        var result: Result<com.kiri.domain.model.taste.MyTaste> = Result.success(com.kiri.domain.model.taste.MyTaste("나", null))
        override suspend fun getMyTaste(): Result<com.kiri.domain.model.taste.MyTaste> { calls++; return result }
        override suspend fun saveMyTaste(taste: com.kiri.domain.model.taste.Taste): Result<com.kiri.domain.model.taste.MyTaste> = error("unused")
    }

    @Test fun invitationUsesRepositoryCodeAndIsBufferedBeforeCollection() = runTest(dispatcher) {
        val repo = TestRepository().apply { home = home.copy(inviteCode = "TESTCODE") }
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.InviteFamilyClicked)
        assertEquals(HomeUiEffect.InviteFamily("TESTCODE"), vm.effect.first())
        assertEquals("TESTCODE", vm.uiState.value.home?.inviteCode)
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

    @Test fun missingInviteCodeIsNotReplacedWithTemporaryCode() = runTest(dispatcher) {
        val vm = HomeViewModel(TestRepository())
        advanceUntilIdle()
        assertNull(vm.uiState.value.home?.inviteCode)
    }

    @Test fun oversizedDraftNeverReachesRepository() = runTest(dispatcher) {
        val repo = TestRepository()
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.AnswerEditClicked)
        vm.onEvent(HomeUiEvent.AnswerChanged("가".repeat(501)))
        vm.onEvent(HomeUiEvent.AnswerSaveClicked)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canSave)
        assertEquals(0, repo.saves)
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

    @Test fun resumingHomeKeepsTheLoadedCardVisibleWhileRefreshing() = runTest(dispatcher) {
        val vm = HomeViewModel(TestRepository())
        advanceUntilIdle()
        val previous = vm.uiState.value.home
        vm.onEvent(HomeUiEvent.CardFlipped)
        vm.refreshOnResume()
        assertFalse(vm.uiState.value.loading)
        assertEquals(previous, vm.uiState.value.home)
        assertTrue(vm.uiState.value.showingMission)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.showingMission)
    }

    @Test fun responseRequiredCardOpensTheResponseScreen() = runTest(dispatcher) {
        val vm = HomeViewModel(TestRepository())
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.ScheduleClicked("120", respond = true))
        assertEquals(HomeUiEffect.OpenSchedule("120", respond = true), vm.effect.first())
        vm.onEvent(HomeUiEvent.ScheduleClicked("118"))
        assertEquals(HomeUiEffect.OpenSchedule("118"), vm.effect.first())
    }

    @Test fun weeklyActionOpensRegistrationOrExistingScheduleAndBlocksCompleted() = runTest(dispatcher) {
        val repo = TestRepository()
        val weekly = WeeklyMission("weekly", "산책", "공원", emptyList(), false, missionId = "mission")
        repo.home = repo.home.copy(mission = weekly)
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        vm.onEvent(HomeUiEvent.WeeklyScheduleClicked)
        assertEquals(HomeUiEffect.CreateWeeklySchedule("weekly"), vm.effect.first())
        repo.home = repo.home.copy(schedules = listOf(HomeSchedule("active", "산책",
            java.time.Instant.now(), ScheduleStatus.PENDING_RESPONSE, true, weeklyMissionId = "weekly")))
        vm.refreshOnResume(); advanceUntilIdle()
        vm.onEvent(HomeUiEvent.WeeklyScheduleClicked)
        assertEquals(HomeUiEffect.OpenSchedule("active"), vm.effect.first())
        repo.home = repo.home.copy(mission = weekly.copy(completed = true))
        vm.refreshOnResume(); advanceUntilIdle()
        vm.onEvent(HomeUiEvent.WeeklyScheduleClicked)
        assertNull(kotlinx.coroutines.withTimeoutOrNull(10) { vm.effect.first() })
    }

    @Test fun onlyLinkedWeeklyReviewCompletesServerBackedHomeAndPreservesServerSchedules() = runTest(dispatcher) {
        val repo = TestRepository()
        repo.home = repo.home.copy(serverBacked = true,
            mission = WeeklyMission("weekly", "산책", "공원", emptyList(), false, missionId = "mission"),
            schedules = listOf(HomeSchedule("remote", "서버 일정", java.time.Instant.now(), ScheduleStatus.CONFIRMED, true)))
        val shared = TestSchedules()
        val vm = HomeViewModel(repo, scheduleRepository = shared)
        advanceUntilIdle()
        val activity = com.kiri.domain.schedule.FamilySchedule("local", "bucket", "mission", "산책", "me",
            java.time.Instant.now(), java.time.Instant.now(), emptyMap(), "공원", completed = true)
        shared.schedules.value = listOf(activity)
        runCurrent()
        assertFalse(vm.uiState.value.home!!.mission!!.completed)
        shared.schedules.value = listOf(activity.copy(weeklyMissionId = "weekly"))
        runCurrent()
        assertTrue(vm.uiState.value.home!!.mission!!.completed)
        assertEquals(setOf("remote", "local"), vm.uiState.value.home!!.schedules.map { it.id }.toSet())
    }

    private class TestSchedules : com.kiri.domain.schedule.ScheduleRepository {
        override val schedules = kotlinx.coroutines.flow.MutableStateFlow<List<com.kiri.domain.schedule.FamilySchedule>>(emptyList())
        override val notices = kotlinx.coroutines.flow.MutableStateFlow<List<com.kiri.domain.schedule.ScheduleNotice>>(emptyList())
        override suspend fun refresh() {}
        override suspend fun create(draft: com.kiri.domain.schedule.ScheduleDraft, requestId: String): com.kiri.domain.schedule.FamilySchedule = error("unused")
        override suspend fun update(id: String, draft: com.kiri.domain.schedule.ScheduleDraft, expectedRevision: Int): com.kiri.domain.schedule.FamilySchedule = error("unused")
        override suspend fun respond(id: String, response: com.kiri.domain.schedule.MemberResponse, expectedRevision: Int): com.kiri.domain.schedule.FamilySchedule = error("unused")
        override suspend fun cancel(id: String, expectedRevision: Int) = error("unused")
        override suspend fun complete(id: String) = error("unused")
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
