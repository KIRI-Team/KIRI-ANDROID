package com.kiri.feature.mypage

import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.feature.mypage.model.nameedit.NameEditUiEffect
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NameEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val familyRepository = TestFamilyRepository()

    // 닉네임·가족명 수정은 동작이 같아서 공통 동작은 닉네임 수정으로 확인한다
    private val viewModel: NameEditViewModel = NicknameEditViewModel(members = null, home = null)
    private val state get() = viewModel.uiState.value

    // 가족 정보 API 대신 정해 둔 가족을 돌려주고, 캐시를 비운 횟수를 센다
    private class FamilyHomeRepository : com.kiri.domain.home.HomeRepository {
        var invalidated = 0
        val family = com.kiri.domain.home.FamilyHome(
            java.time.LocalDate.of(2026, 11, 10), "1", "끼리네가족",
            listOf(com.kiri.domain.home.FamilyMember("1", "쥬123", "딸"), com.kiri.domain.home.FamilyMember("2", "마마", "엄마")),
            null, emptyMap(), emptySet(), null, serverBacked = true,
        )
        override suspend fun load() = family
        override suspend fun saveAnswer(question: com.kiri.domain.home.DailyQuestion, answer: String) = family
        override suspend fun poke(question: com.kiri.domain.home.DailyQuestion, memberId: String) = family
        override suspend fun invalidateFamily() { invalidated++ }
    }

    @Test
    fun 닉네임_수정은_입력이_비어_있고_가족_정보가_없으면_지금_닉네임도_비어_있다() {
        assertEquals("", state.currentName)
        assertEquals("", state.name)
        assertTrue(state.violations.isEmpty())
    }

    @Test
    fun 지금_닉네임과_가족명을_가족_정보에서_불러온다() = runTest(mainDispatcherRule.testDispatcher) {
        val home = FamilyHomeRepository()
        val nickname = NicknameEditViewModel(members = null, home = home)
        val familyName = FamilyNameEditViewModel(familyRepository, home)
        advanceUntilIdle()

        assertEquals("쥬123", nickname.uiState.value.currentName)
        assertEquals("끼리네가족", familyName.uiState.value.currentName)
    }

    // 닉네임 변경 결과를 정해 두고 보낸 닉네임을 모은다
    private class Members(var error: Throwable? = null) : com.kiri.domain.repository.MemberRepository {
        val sent = mutableListOf<String>()
        override suspend fun updateNickname(nickname: String): Result<Unit> {
            sent += nickname
            return error?.let { Result.failure(it) } ?: Result.success(Unit)
        }
        override suspend fun updateFamilyRole(role: com.kiri.domain.model.member.FamilyRole): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun 가족이_없어도_지금_닉네임을_내_정보에서_불러온다() = runTest(mainDispatcherRule.testDispatcher) {
        val members = object : com.kiri.domain.repository.MemberRepository {
            override suspend fun updateNickname(nickname: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateFamilyRole(role: com.kiri.domain.model.member.FamilyRole): Result<Unit> = Result.success(Unit)
            override suspend fun me() = Result.success(com.kiri.domain.model.member.MyInfo(5, "둘러보는중", null, null, null))
        }
        val nickname = NicknameEditViewModel(members, home = null)
        advanceUntilIdle()

        assertEquals("둘러보는중", nickname.uiState.value.currentName)
    }

    @Test
    fun 닉네임을_서버에_저장하고_가족_정보_캐시를_비운_뒤_돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        val members = Members()
        val home = FamilyHomeRepository()
        val nickname = NicknameEditViewModel(members, home)
        nickname.onEvent(NameEditUiEvent.NameChanged("새닉네임"))
        nickname.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()

        assertEquals(listOf("새닉네임"), members.sent)
        assertEquals(1, home.invalidated)
        assertEquals(NameEditUiEffect.NavigateBack, nickname.effect.first())
    }

    @Test
    fun 이미_쓰는_닉네임이면_중복으로_표시하고_다른_실패는_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        val members = Members(com.kiri.domain.model.nickname.DuplicateNicknameException())
        val home = FamilyHomeRepository()
        val nickname = NicknameEditViewModel(members, home)
        nickname.onEvent(NameEditUiEvent.NameChanged("새닉네임"))
        nickname.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()
        assertTrue(nickname.uiState.value.isDuplicated)
        assertEquals(0, home.invalidated)

        members.error = IOException()
        nickname.onEvent(NameEditUiEvent.NameChanged("다른닉네임"))
        nickname.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()
        assertFalse(nickname.uiState.value.isDuplicated)
        assertEquals(NameEditUiEffect.ShowSaveError, nickname.effect.first())
    }

    @Test
    fun 방장이_아니면_가족명을_바꾸지_못한다고_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        familyRepository.renameError = com.kiri.domain.model.family.NotFamilyOwnerException()
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)
        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족2"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()

        assertEquals(NameEditUiEffect.ShowMessage("방장만 가족명을 바꿀 수 있어요."), familyNameViewModel.effect.first())
        assertFalse(familyNameViewModel.uiState.value.isDuplicated)
    }

    @Test
    fun 가족명을_저장하면_가족_정보_캐시를_비운다() = runTest(mainDispatcherRule.testDispatcher) {
        val home = FamilyHomeRepository()
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository, home)
        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족2"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()

        assertEquals(1, home.invalidated)
    }

    @Test
    fun 비어_있는_채로_다음을_누르면_길이_오류를_표시하고_머문다() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.LENGTH), state.violations)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 규칙을_지키지_못한_이름은_오류를_표시하고_머문다() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onEvent(NameEditUiEvent.NameChanged("쥬!"))
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.NO_SPECIAL_CHARACTERS), state.violations)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 가족명에_공백이_있으면_특수_문자_오류다() {
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네 가족"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.NO_SPECIAL_CHARACTERS), familyNameViewModel.uiState.value.violations)
    }

    @Test
    fun 닉네임_수정은_12자까지_허용한다() {
        viewModel.onEvent(NameEditUiEvent.NameChanged("가".repeat(13)))
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.LENGTH), state.violations)
        assertEquals(12, state.maxLength)
    }

    @Test
    fun 가족명_수정은_15자까지_허용한다() = runTest(mainDispatcherRule.testDispatcher) {
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("가".repeat(15)))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)

        assertTrue(familyNameViewModel.uiState.value.violations.isEmpty())
        assertEquals(15, familyNameViewModel.uiState.value.maxLength)
        assertEquals(NameEditUiEffect.NavigateBack, familyNameViewModel.effect.first())
    }

    @Test
    fun 가족명이_15자를_넘으면_길이_오류를_표시한다() {
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("가".repeat(16)))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.LENGTH), familyNameViewModel.uiState.value.violations)
    }

    @Test
    fun 입력을_바꾸면_이전_오류를_비운다() {
        viewModel.onEvent(NameEditUiEvent.NameChanged("쥬"))
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        viewModel.onEvent(NameEditUiEvent.NameChanged("쥬1"))

        assertEquals("쥬1", state.name)
        assertTrue(state.violations.isEmpty())
    }

    @Test
    fun 규칙을_지킨_이름으로_다음을_누르면_이전_화면으로_돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onEvent(NameEditUiEvent.NameChanged("끼리123"))
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        assertTrue(state.violations.isEmpty())
        assertEquals(NameEditUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 규칙을_지킨_가족명은_서버에_저장하고_이전_화면으로_돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족2"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(NameEditUiEffect.NavigateBack, familyNameViewModel.effect.first())
        assertEquals(listOf("끼리네가족2"), familyRepository.renamedNames)
    }

    @Test
    fun 규칙을_지키지_못한_가족명은_서버에_보내지_않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()

        assertTrue(familyRepository.renamedNames.isEmpty())
    }

    @Test
    fun 이미_있는_가족명이면_중복을_표시하고_머문다() = runTest(mainDispatcherRule.testDispatcher) {
        familyRepository.renameError = DuplicateFamilyNameException()
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족2"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()

        assertTrue(familyNameViewModel.uiState.value.isDuplicated)
        assertNull(withTimeoutOrNull(100) { familyNameViewModel.effect.first() })
    }

    @Test
    fun 가족명을_바꾸면_중복_표시를_비운다() = runTest(mainDispatcherRule.testDispatcher) {
        familyRepository.renameError = DuplicateFamilyNameException()
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)
        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족2"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족3"))

        assertFalse(familyNameViewModel.uiState.value.isDuplicated)
    }

    @Test
    fun 가족명을_저장하지_못하면_머물며_오류를_알린다() = runTest(mainDispatcherRule.testDispatcher) {
        familyRepository.renameError = IOException()
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족2"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(NameEditUiEffect.ShowSaveError, familyNameViewModel.effect.first())
        assertFalse(familyNameViewModel.uiState.value.isDuplicated)
    }

    @Test
    fun 저장_중에_다시_눌러도_한_번만_보낸다() = runTest(mainDispatcherRule.testDispatcher) {
        val familyNameViewModel = FamilyNameEditViewModel(familyRepository)

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네가족2"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)
        advanceUntilIdle()

        assertEquals(1, familyRepository.renamedNames.size)
    }

    @Test
    fun 뒤로가기를_누르면_이전_화면으로_돌아간다() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onEvent(NameEditUiEvent.BackClicked)

        assertEquals(NameEditUiEffect.NavigateBack, viewModel.effect.first())
    }
}
