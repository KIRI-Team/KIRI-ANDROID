package com.kiri.feature.mypage

import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.feature.mypage.model.nameedit.NameEditUiEffect
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NameEditViewModelTest {

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
        assertEquals(MypageProfile.Sample.familyName, FamilyNameEditViewModel().uiState.value.currentName)
    }

    @Test
    fun 비어_있는_채로_다음을_누르면_길이_오류를_표시하고_머문다() = runTest {
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.LENGTH), state.violations)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 규칙을_지키지_못한_이름은_오류를_표시하고_머문다() = runTest {
        viewModel.onEvent(NameEditUiEvent.NameChanged("쥬!"))
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.NO_SPECIAL_CHARACTERS), state.violations)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 가족명에_공백이_있으면_특수_문자_오류다() {
        val familyNameViewModel = FamilyNameEditViewModel()

        familyNameViewModel.onEvent(NameEditUiEvent.NameChanged("끼리네 가족"))
        familyNameViewModel.onEvent(NameEditUiEvent.NextClicked)

        assertEquals(setOf(NicknameRule.NO_SPECIAL_CHARACTERS), familyNameViewModel.uiState.value.violations)
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
    fun 규칙을_지킨_이름으로_다음을_누르면_이전_화면으로_돌아간다() = runTest {
        viewModel.onEvent(NameEditUiEvent.NameChanged("끼리123"))
        viewModel.onEvent(NameEditUiEvent.NextClicked)

        assertTrue(state.violations.isEmpty())
        assertEquals(NameEditUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 뒤로가기를_누르면_이전_화면으로_돌아간다() = runTest {
        viewModel.onEvent(NameEditUiEvent.BackClicked)

        assertEquals(NameEditUiEffect.NavigateBack, viewModel.effect.first())
    }
}
