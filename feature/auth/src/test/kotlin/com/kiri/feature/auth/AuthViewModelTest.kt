package com.kiri.feature.auth

import com.kiri.feature.auth.model.AgreementTerm
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
    fun 상세_링크가_없는_약관은_눌러도_아무_동작을_하지_않는다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_1))

        assertNull(withTimeoutOrNull(1_000) { viewModel.effect.first() })
    }
}
