package com.kiri.feature.mypage

import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.feature.mypage.model.common.MypageProfile
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
    private val viewModel: NameEditViewModel = NicknameEditViewModel()
    private val state get() = viewModel.uiState.value

    @Test
    fun 닉네임_수정은_지금_닉네임을_보여주고_입력은_비어_있다() {
        assertEquals(MypageProfile.Sample.nickname, state.currentName)
        assertEquals("", state.name)
        assertTrue(state.violations.isEmpty())
    }

    @Test
    fun 가족명_수정은_지금_가족명을_보여준다() {
        assertEquals(MypageProfile.Sample.familyName, FamilyNameEditViewModel(familyRepository).uiState.value.currentName)
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
