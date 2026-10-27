package com.kiri.feature.auth

import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.model.term.Term
import com.kiri.domain.repository.AuthRepository
import com.kiri.domain.repository.TermRepository
import com.kiri.feature.auth.login.NonceGenerator
import com.kiri.feature.auth.login.SocialLoginResult
import com.kiri.feature.auth.model.AuthUiEffect
import com.kiri.feature.auth.model.AuthUiEvent
import com.kiri.feature.auth.model.LoginProvider
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
        override val isLoggedIn: Flow<Boolean> = emptyFlow()

        override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> {
            calls += Triple(provider, socialToken, nonce)
            return result
        }
    }

    private class FakeTermRepository : TermRepository {
        var termsResult: Result<List<Term>> = Result.success(TERMS)
        var agreeResult: Result<Unit> = Result.success(Unit)

        // 값을 넣으면 응답을 그때까지 붙잡아 둔다 (요청 중 상태를 확인할 때 쓴다)
        var termsGate: CompletableDeferred<Unit>? = null
        var agreeGate: CompletableDeferred<Unit>? = null
        var getTermsCalls = 0
        val agreeCalls = mutableListOf<Map<Long, Boolean>>()

        override suspend fun getTerms(): Result<List<Term>> {
            getTermsCalls++
            termsGate?.await()
            return termsResult
        }

        override suspend fun agree(agreements: Map<Long, Boolean>): Result<Unit> {
            agreeCalls += agreements
            agreeGate?.await()
            return agreeResult
        }
    }

    private val repository = FakeAuthRepository()
    private val termRepository = FakeTermRepository()
    private val viewModel = AuthViewModel(repository, termRepository, NonceGenerator())
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
    fun 약관동의로_넘어가면_약관_목록을_받는다() = runTest {
        moveToTerms()

        assertEquals(TERMS, state.terms)
        assertFalse(state.isLoadingTerms)
        assertFalse(state.termsLoadFailed)
    }

    @Test
    fun 약관_목록을_받는_동안_로딩을_보여주고_계속할_수_없다() = runTest {
        termRepository.termsGate = CompletableDeferred()

        moveToTerms()

        assertTrue(state.isLoadingTerms)
        assertFalse(state.canContinue)
    }

    @Test
    fun 약관에_동의한_기존_회원은_약관_목록을_받지_않는다() = runTest {
        repository.result = Result.success(LoginResult(isNewUser = false, termsAgreed = true))

        login()

        assertEquals(0, termRepository.getTermsCalls)
    }

    @Test
    fun 약관_목록을_받지_못하면_실패를_보여주고_다시_시도하면_다시_받는다() = runTest {
        termRepository.termsResult = Result.failure(IOException("offline"))
        moveToTerms()
        assertTrue(state.termsLoadFailed)
        assertTrue(state.terms.isEmpty())
        assertFalse(state.canContinue)

        termRepository.termsResult = Result.success(TERMS)
        viewModel.onEvent(AuthUiEvent.TermsRetryClicked)

        assertEquals(TERMS, state.terms)
        assertFalse(state.termsLoadFailed)
        assertEquals(2, termRepository.getTermsCalls)
    }

    @Test
    fun 로그인으로_돌아갔다_다시_와도_받은_약관_목록과_동의_상태를_그대로_쓴다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermToggled(SERVICE))
        viewModel.onEvent(AuthUiEvent.BackClicked)

        login()

        assertEquals(AuthStep.TERMS_AGREEMENT, state.currentStep)
        assertEquals(1, termRepository.getTermsCalls)
        assertEquals(setOf(SERVICE.id), state.agreedTermIds)
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
        assertEquals(TERMS.map(Term::id).toSet(), state.agreedTermIds)

        viewModel.onEvent(AuthUiEvent.AllTermsToggled)
        assertTrue(state.agreedTermIds.isEmpty())
    }

    @Test
    fun 개별_약관을_모두_선택하면_전체동의_상태가_된다() = runTest {
        moveToTerms()

        TERMS.forEach { viewModel.onEvent(AuthUiEvent.TermToggled(it)) }

        assertTrue(state.allAgreed)
        assertTrue(state.canContinue)
    }

    @Test
    fun 필수_약관에만_동의해도_계속할_수_있지만_전체동의는_아니다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermToggled(SERVICE))
        viewModel.onEvent(AuthUiEvent.TermToggled(PRIVACY))

        assertTrue(state.canContinue)
        assertFalse(state.allAgreed)
    }

    @Test
    fun 필수_약관을_하나라도_해제하면_계속할_수_없다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.AllTermsToggled)

        viewModel.onEvent(AuthUiEvent.TermToggled(SERVICE))

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
    fun 동의하면_선택_약관_미동의까지_약관마다_동의_여부를_보낸다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermToggled(SERVICE))
        viewModel.onEvent(AuthUiEvent.TermToggled(PRIVACY))

        viewModel.onEvent(AuthUiEvent.AgreeClicked)

        assertEquals(listOf(mapOf(SERVICE.id to true, PRIVACY.id to true, MARKETING.id to false)), termRepository.agreeCalls)
    }

    @Test
    fun 동의를_저장하는_동안에는_다시_눌러도_한_번만_보내고_뒤로가지_않는다() = runTest {
        termRepository.agreeGate = CompletableDeferred()
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.AllTermsToggled)

        viewModel.onEvent(AuthUiEvent.AgreeClicked)
        viewModel.onEvent(AuthUiEvent.AgreeClicked)
        viewModel.onEvent(AuthUiEvent.BackClicked)

        assertTrue(state.isAgreeing)
        assertFalse(state.canContinue)
        assertEquals(1, termRepository.agreeCalls.size)
        assertEquals(AuthStep.TERMS_AGREEMENT, state.currentStep)
    }

    @Test
    fun 동의_저장에_실패하면_오류를_안내하고_동의_상태를_유지한다() = runTest {
        val cause = IOException("offline")
        termRepository.agreeResult = Result.failure(cause)
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.AllTermsToggled)

        viewModel.onEvent(AuthUiEvent.AgreeClicked)

        assertEquals(AuthUiEffect.ShowAgreeError(cause), viewModel.effect.first())
        assertFalse(state.isAgreeing)
        assertTrue(state.canContinue)
        assertEquals(TERMS.map(Term::id).toSet(), state.agreedTermIds)
        assertEquals(AuthStep.TERMS_AGREEMENT, state.currentStep)
    }

    @Test
    fun 필수_약관에_동의하지_않으면_계속해도_보내지_않고_이동하지_않는다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.AgreeClicked)

        assertTrue(termRepository.agreeCalls.isEmpty())
        assertNull(withTimeoutOrNull(1_000) { viewModel.effect.first() })
    }

    @Test
    fun 약관_화살표를_누르면_해당_약관의_세부_내용_창이_열린다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermDetailClicked(PRIVACY))

        assertEquals(PRIVACY, viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 세부_내용을_열어도_동의_상태는_바뀌지_않는다() = runTest {
        moveToTerms()

        viewModel.onEvent(AuthUiEvent.TermDetailClicked(PRIVACY))

        assertEquals(emptySet<Long>(), viewModel.uiState.value.agreedTermIds)
    }

    @Test
    fun 세부_내용_창을_닫으면_보고_있던_약관이_비워진다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(PRIVACY))

        viewModel.onEvent(AuthUiEvent.TermDetailDismissed)

        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 약관동의에서_로그인으로_돌아가면_세부_내용_창도_닫힌다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(PRIVACY))

        viewModel.onEvent(AuthUiEvent.BackClicked)

        assertEquals(AuthStep.LOGIN, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.viewingTerm)
    }

    @Test
    fun 세부_내용_창에서_동의하기를_누르면_해당_약관에_동의하고_창이_닫힌다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(PRIVACY))

        viewModel.onEvent(AuthUiEvent.TermDetailAgreed(PRIVACY))

        assertEquals(setOf(PRIVACY.id), state.agreedTermIds)
        assertNull(state.viewingTerm)
    }

    @Test
    fun 이미_동의한_약관을_세부_내용_창에서_동의해도_해제되지_않는다() = runTest {
        moveToTerms()
        viewModel.onEvent(AuthUiEvent.TermToggled(PRIVACY))
        viewModel.onEvent(AuthUiEvent.TermDetailClicked(PRIVACY))

        viewModel.onEvent(AuthUiEvent.TermDetailAgreed(PRIVACY))

        assertEquals(setOf(PRIVACY.id), state.agreedTermIds)
    }

    private companion object {
        val SERVICE = Term(id = 1, title = "서비스 이용약관", content = "서비스 본문", required = true)
        val PRIVACY = Term(id = 2, title = "개인정보 처리방침", content = "개인정보 본문", required = true)
        val MARKETING = Term(id = 3, title = "마케팅 정보 수신 동의", content = "마케팅 본문", required = false)
        val TERMS = listOf(SERVICE, PRIVACY, MARKETING)
    }
}
