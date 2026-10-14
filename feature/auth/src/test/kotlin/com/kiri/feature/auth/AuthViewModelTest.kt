package com.kiri.feature.auth

import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.model.term.AgreementTerm
import com.kiri.domain.repository.AuthRepository
import com.kiri.feature.auth.login.NonceGenerator
import com.kiri.feature.auth.login.SocialLoginResult
import com.kiri.feature.auth.model.AuthUiEffect
import com.kiri.feature.auth.model.AuthUiEvent
import com.kiri.feature.auth.model.LoginProvider
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private class FakeAuthRepository : AuthRepository {
        var result: Result<LoginResult> = Result.success(LoginResult(isNewUser = true, termsAgreed = false))
        val calls = mutableListOf<Triple<SocialProvider, String, String>>()

        override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> {
            calls += Triple(provider, socialToken, nonce)
            return result
        }
    }

    private val repository = FakeAuthRepository()
    private val viewModel = AuthViewModel(repository, NonceGenerator())
    private val state get() = viewModel.uiState.value

    // 로그인 버튼 → SDK 로그인 시작 요청을 받고 → SDK 결과를 돌려준다
    private suspend fun login(
        sdkResult: SocialLoginResult = SocialLoginResult.Success(LoginProvider.KAKAO, "id-token"),
        provider: LoginProvider = LoginProvider.KAKAO,
    ): AuthUiEffect.StartSocialLogin {
        viewModel.onEvent(AuthUiEvent.LoginClicked(provider))
        val start = viewModel.effect.first() as AuthUiEffect.StartSocialLogin
        viewModel.onEvent(AuthUiEvent.SocialLoginFinished(sdkResult))
        return start
    }

    private suspend fun moveToTerms() {
        login()
    }

    @Test
    fun 로그인을_누르면_nonce와_함께_SDK_로그인을_요청하고_버튼을_막는다() = runTest {
        viewModel.onEvent(AuthUiEvent.LoginClicked(LoginProvider.GOOGLE))

        val effect = viewModel.effect.first() as AuthUiEffect.StartSocialLogin
        assertEquals(LoginProvider.GOOGLE, effect.provider)
        assertTrue(effect.nonce.isNotBlank())
        assertTrue(state.isLoggingIn)
    }

    @Test
    fun 로그인_중에_다시_눌러도_한_번만_요청한다() = runTest {
        viewModel.onEvent(AuthUiEvent.LoginClicked(LoginProvider.KAKAO))
        viewModel.effect.first()

        viewModel.onEvent(AuthUiEvent.LoginClicked(LoginProvider.GOOGLE))

        assertNull(withTimeoutOrNull(1_000) { viewModel.effect.first() })
    }

    @Test
    fun SDK_로그인에_성공하면_ID_토큰과_같은_nonce로_서버에_로그인한다() = runTest {
        val start = login(SocialLoginResult.Success(LoginProvider.GOOGLE, "google-id-token"), LoginProvider.GOOGLE)

        assertEquals(listOf(Triple(SocialProvider.GOOGLE, "google-id-token", start.nonce)), repository.calls)
    }

    @Test
    fun 로그인할_때마다_새_nonce를_쓴다() = runTest {
        val first = login(SocialLoginResult.Cancelled)
        val second = login(SocialLoginResult.Cancelled)

        assertTrue(first.nonce != second.nonce)
    }

    @Test
    fun 새_회원이면_약관동의로_이동한다() = runTest {
        login()

        assertEquals(AuthStep.TERMS_AGREEMENT, state.currentStep)
        assertFalse(state.isLoggingIn)
    }

    @Test
    fun 약관에_동의한_기존_회원은_바로_메인으로_이동한다() = runTest {
        repository.result = Result.success(LoginResult(isNewUser = false, termsAgreed = true))

        login()

        assertEquals(AuthUiEffect.NavigateToMain, viewModel.effect.first())
        assertEquals(AuthStep.LOGIN, state.currentStep)
    }

    @Test
    fun 약관에_동의하지_않은_기존_회원은_약관동의_후_메인으로_이동한다() = runTest {
        repository.result = Result.success(LoginResult(isNewUser = false, termsAgreed = false))
        login()
        assertEquals(AuthStep.TERMS_AGREEMENT, state.currentStep)
        viewModel.onEvent(AuthUiEvent.AllTermsToggled)

        viewModel.onEvent(AuthUiEvent.AgreeClicked)

        assertEquals(AuthUiEffect.NavigateToMain, viewModel.effect.first())
    }

    @Test
    fun SDK_로그인을_취소하면_안내_없이_버튼만_다시_풀린다() = runTest {
        login(SocialLoginResult.Cancelled)

        assertFalse(state.isLoggingIn)
        assertEquals(AuthStep.LOGIN, state.currentStep)
        assertTrue(repository.calls.isEmpty())
        assertNull(withTimeoutOrNull(1_000) { viewModel.effect.first() })
    }

    @Test
    fun SDK_로그인에_실패하면_오류를_안내하고_서버에_보내지_않는다() = runTest {
        val cause = IllegalStateException("no key")

        login(SocialLoginResult.Failure(LoginProvider.KAKAO, cause))

        assertEquals(AuthUiEffect.ShowLoginError(cause), viewModel.effect.first())
        assertFalse(state.isLoggingIn)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun 서버_로그인에_실패하면_오류를_안내하고_로그인_화면에_남는다() = runTest {
        val cause = IOException("offline")
        repository.result = Result.failure(cause)

        login()

        assertEquals(AuthUiEffect.ShowLoginError(cause), viewModel.effect.first())
        assertFalse(state.isLoggingIn)
        assertEquals(AuthStep.LOGIN, state.currentStep)
    }

    @Test
    fun 요청하지_않은_SDK_결과는_무시한다() = runTest {
        viewModel.onEvent(AuthUiEvent.SocialLoginFinished(SocialLoginResult.Success(LoginProvider.KAKAO, "token")))

        assertTrue(repository.calls.isEmpty())
        assertEquals(AuthStep.LOGIN, state.currentStep)
    }

    @Test
    fun 로그인_화면에서_시작한다() {
        assertEquals(AuthStep.LOGIN, state.currentStep)
    }

    @Test
    fun 약관동의에서_뒤로가면_로그인으로_돌아간다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.BackClicked)

        assertEquals(AuthStep.LOGIN, state.currentStep)
    }

    @Test
    fun 전체동의를_누르면_모든_약관이_선택되고_다시_누르면_모두_해제된다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.AllTermsToggled)
        assertTrue(state.allAgreed)
        assertEquals(AgreementTerm.entries.toSet(), state.agreedTerms)

        viewModel.onEvent(AuthUiEvent.AllTermsToggled)
        assertTrue(state.agreedTerms.isEmpty())
    }

    @Test
    fun 개별_약관을_모두_선택하면_전체동의_상태가_된다() = runTest {
        moveToTerms()

        AgreementTerm.entries.forEach { viewModel.onEvent(AuthUiEvent.TermToggled(it)) }

        assertTrue(state.allAgreed)
        assertTrue(state.canContinue)
    }

    @Test
    fun 필수_약관을_하나라도_해제하면_계속할_수_없다() = runTest {
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
    fun 약관_화살표를_누르면_해당_약관의_세부_내용_창이_열린다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        assertEquals(AgreementTerm.TERM_2, viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 세부_내용을_열어도_동의_상태는_바뀌지_않는다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        assertEquals(emptySet<AgreementTerm>(), viewModel.uiState.value.agreedTerms)
    }

    @Test
    fun 세부_내용_창을_닫으면_보고_있던_약관이_비워진다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.TermDetailDismissed)

        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 약관동의에서_로그인으로_돌아가면_세부_내용_창도_닫힌다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.BackClicked)

        assertEquals(AuthStep.LOGIN, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 세부_내용_창에서_동의하기를_누르면_해당_약관에_동의하고_창이_닫힌다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.TermDetailAgreed(AgreementTerm.TERM_2))

        assertEquals(setOf(AgreementTerm.TERM_2), state.agreedTerms)
        assertNull(state.viewingTerm)
    }

    @Test
    fun 이미_동의한_약관을_세부_내용_창에서_동의해도_해제되지_않는다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermToggled(AgreementTerm.TERM_2))
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(AgreementTerm.TERM_2))

        viewModel.onEvent(AuthUiEvent.TermDetailAgreed(AgreementTerm.TERM_2))

        assertEquals(setOf(AgreementTerm.TERM_2), state.agreedTerms)
    }
}
