package com.kiri.feature.auth

import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.auth.model.AuthUiEffect
import com.kiri.feature.auth.model.AuthUiEvent
import com.kiri.feature.auth.model.LoginProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {

    private val viewModel = AuthViewModel()
    private val state get() = viewModel.uiState.value

    private fun moveToTerms() {
        viewModel.onEvent(AuthUiEvent.LoginClicked(LoginProvider.KAKAO))
    }

    @Test
    fun 로그인_화면에서_시작한다() {
        assertEquals(AuthStep.LOGIN, state.currentStep)
    }

    @Test
    fun 소셜_로그인을_누르면_약관동의로_이동한다() {
        moveToTerms()

        assertEquals(AuthStep.TERMS_AGREEMENT, state.currentStep)
    }

    @Test
    fun 약관동의에서_뒤로가면_로그인으로_돌아간다() {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.BackClicked)

        assertEquals(AuthStep.LOGIN, state.currentStep)
    }

    @Test
    fun 전체동의를_누르면_모든_약관이_선택되고_다시_누르면_모두_해제된다() {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.AllTermsToggled)
        assertTrue(state.allAgreed)
        assertEquals(AgreementTerm.entries.toSet(), state.agreedTerms)

        viewModel.onEvent(AuthUiEvent.AllTermsToggled)
        assertTrue(state.agreedTerms.isEmpty())
    }

    @Test
    fun 개별_약관을_모두_선택하면_전체동의_상태가_된다() {
        moveToTerms()

        AgreementTerm.entries.forEach { viewModel.onEvent(AuthUiEvent.TermToggled(it)) }

        assertTrue(state.allAgreed)
        assertTrue(state.canContinue)
    }

    @Test
    fun 필수_약관을_하나라도_해제하면_계속할_수_없다() {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.AllTermsToggled)

        viewModel.onEvent(AuthUiEvent.TermToggled(AgreementTerm.TERM_1))

        assertFalse(state.allAgreed)
        assertFalse(state.canContinue)
    }

    @Test
    fun 필수_약관에_모두_동의하고_계속하면_온보딩으로_이동한다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.AllTermsToggled)

        viewModel.onEvent(AuthUiEvent.AgreeClicked)

        assertEquals(AuthUiEffect.NavigateToOnboarding, viewModel.effect.first())
    }

    @Test
    fun 필수_약관에_동의하지_않으면_계속해도_이동하지_않는다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.AgreeClicked)

        assertNull(withTimeoutOrNull(1_000) { viewModel.effect.first() })
    }

    @Test
    fun 약관_화살표를_누르면_해당_약관의_세부_내용_창이_열린다() {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        assertEquals(AgreementTerm.TERM_2, viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 세부_내용을_열어도_동의_상태는_바뀌지_않는다() {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        assertEquals(emptySet<AgreementTerm>(), viewModel.uiState.value.agreedTerms)
    }

    @Test
    fun 세부_내용_창을_닫으면_보고_있던_약관이_비워진다() {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.TermDetailDismissed)

        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 약관동의에서_로그인으로_돌아가면_세부_내용_창도_닫힌다() {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.BackClicked)

        assertEquals(AuthStep.LOGIN, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 세부_내용_창에서_동의하기를_누르면_해당_약관에_동의하고_창이_닫힌다() {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.TermDetailAgreed(AgreementTerm.TERM_2))

        assertEquals(setOf(AgreementTerm.TERM_2), state.agreedTerms)
        assertNull(state.viewingTerm)
    }

    @Test
    fun 이미_동의한_약관을_세부_내용_창에서_동의해도_해제되지_않는다() {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermToggled(AgreementTerm.TERM_2))
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.TermDetailAgreed(AgreementTerm.TERM_2))

        assertEquals(setOf(AgreementTerm.TERM_2), state.agreedTerms)
    }
}
