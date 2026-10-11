package com.kiri.feature.mypage

import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.mypage.model.terms.TermsUiEffect
import com.kiri.feature.mypage.model.terms.TermsUiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TermsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = TermsViewModel()

    @Test
    fun 회원가입과_같은_약관_목록과_최종_업데이트일을_보여준다() {
        val state = viewModel.uiState.value

        assertEquals(AgreementTerm.entries, state.terms)
        assertEquals("최종 업데이트 | 26. 06. 26", state.lastUpdatedText)
        assertNull(state.viewingTerm)
    }

    @Test
    fun 약관을_누르면_세부_내용을_열고_닫으면_비운다() {
        viewModel.onEvent(TermsUiEvent.TermClicked(AgreementTerm.TERM_2))
        assertEquals(AgreementTerm.TERM_2, viewModel.uiState.value.viewingTerm)

        viewModel.onEvent(TermsUiEvent.TermDetailClosed)
        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 뒤로가기를_누르면_돌아간다() = runTest {
        viewModel.onEvent(TermsUiEvent.BackClicked)

        assertEquals(TermsUiEffect.NavigateBack, viewModel.effect.first())
    }
}
