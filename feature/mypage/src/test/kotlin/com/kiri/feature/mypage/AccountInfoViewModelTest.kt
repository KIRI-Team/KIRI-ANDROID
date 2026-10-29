package com.kiri.feature.mypage

import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.repository.AuthRepository
import com.kiri.feature.mypage.model.account.AccountInfoUiEffect
import com.kiri.feature.mypage.model.account.AccountInfoUiEvent
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

class AccountInfoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        var logoutResult: Result<Unit> = Result.success(Unit)
        var logoutCalls = 0

        // 값을 넣으면 응답을 그때까지 붙잡아 둔다 (요청 중 상태를 확인할 때 쓴다)
        var logoutGate: CompletableDeferred<Unit>? = null

        override val isLoggedIn: Flow<Boolean> = emptyFlow()

        override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> =
            error("마이페이지에서는 로그인하지 않는다")

        override suspend fun logout(): Result<Unit> {
            logoutCalls++
            logoutGate?.await()
            return logoutResult
        }

        override suspend fun withdraw(): Result<Unit> = error("계정 정보 화면에서는 탈퇴하지 않는다")
    }

    private val authRepository = FakeAuthRepository()
    private val viewModel = AccountInfoViewModel(authRepository)

    @Test
    fun 메뉴를_누르면_해당_화면으로_이동한다() = runTest {
        val cases = listOf(
            AccountInfoUiEvent.TermsClicked to AccountInfoUiEffect.NavigateToTerms,
            AccountInfoUiEvent.OpenLicenseClicked to AccountInfoUiEffect.NavigateToOpenLicense,
            AccountInfoUiEvent.WithdrawClicked to AccountInfoUiEffect.NavigateToWithdraw,
            AccountInfoUiEvent.BackClicked to AccountInfoUiEffect.NavigateBack,
        )
        cases.forEach { (event, effect) ->
            viewModel.onEvent(event)
            assertEquals(effect, viewModel.effect.first())
        }
    }

    @Test
    fun 로그아웃을_누르면_바로_나가지_않고_확인_팝업을_띄운다() = runTest {
        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)

        assertTrue(viewModel.uiState.value.isLogoutDialogVisible)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 팝업에서_취소하면_팝업만_닫힌다() = runTest {
        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)
        viewModel.onEvent(AccountInfoUiEvent.LogoutCanceled)

        assertFalse(viewModel.uiState.value.isLogoutDialogVisible)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 팝업에서_확인하면_팝업을_닫고_로그아웃한_뒤_로그인_화면으로_간다() = runTest {
        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)

        assertFalse(viewModel.uiState.value.isLogoutDialogVisible)
        assertEquals(AccountInfoUiEffect.NavigateToLogin, viewModel.effect.first())
        assertEquals(1, authRepository.logoutCalls)
    }

    @Test
    fun 로그아웃이_끝나기_전에는_로그인_화면으로_가지_않는다() = runTest {
        authRepository.logoutGate = CompletableDeferred()
        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoggingOut)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })

        authRepository.logoutGate!!.complete(Unit)
        assertEquals(AccountInfoUiEffect.NavigateToLogin, viewModel.effect.first())
    }

    @Test
    fun 로그아웃_요청이_실패해도_토큰은_지워졌으므로_로그인_화면으로_간다() = runTest {
        authRepository.logoutResult = Result.failure(IOException("연결 끊김"))
        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)

        assertEquals(AccountInfoUiEffect.NavigateToLogin, viewModel.effect.first())
    }

    @Test
    fun 로그아웃_중에는_팝업을_다시_띄우지_않아_두_번_요청하지_않는다() = runTest {
        authRepository.logoutGate = CompletableDeferred()
        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)
        advanceUntilIdle()

        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLogoutDialogVisible)
        assertEquals(1, authRepository.logoutCalls)
    }

    @Test
    fun 팝업이_없을_때_온_확인은_무시한다() = runTest {
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)

        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
        assertEquals(0, authRepository.logoutCalls)
    }
}
