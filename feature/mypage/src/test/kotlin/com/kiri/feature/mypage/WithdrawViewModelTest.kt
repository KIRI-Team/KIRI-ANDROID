package com.kiri.feature.mypage

import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.repository.AuthRepository
import com.kiri.feature.mypage.model.withdraw.WithdrawUiEffect
import com.kiri.feature.mypage.model.withdraw.WithdrawUiEvent
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

class WithdrawViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        var withdrawResult: Result<Unit> = Result.success(Unit)
        var withdrawCalls = 0

        // 값을 넣으면 응답을 그때까지 붙잡아 둔다 (요청 중 상태를 확인할 때 쓴다)
        var withdrawGate: CompletableDeferred<Unit>? = null

        override val isLoggedIn: Flow<Boolean> = emptyFlow()

        override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> =
            error("회원탈퇴 화면에서는 로그인하지 않는다")

        override suspend fun logout(): Result<Unit> = error("회원탈퇴 화면에서는 로그아웃하지 않는다")

        override suspend fun withdraw(): Result<Unit> {
            withdrawCalls++
            withdrawGate?.await()
            return withdrawResult
        }
    }

    private val authRepository = FakeAuthRepository()
    private val viewModel = WithdrawViewModel(authRepository)

    @Test
    fun 탈퇴할게요를_누르면_탈퇴한_뒤_로그인_화면으로_간다() = runTest {
        viewModel.onEvent(WithdrawUiEvent.WithdrawClicked)

        assertEquals(WithdrawUiEffect.NavigateToLogin, viewModel.effect.first())
        assertEquals(1, authRepository.withdrawCalls)
    }

    @Test
    fun 탈퇴_요청_중에는_다시_눌러도_한_번만_요청한다() = runTest {
        authRepository.withdrawGate = CompletableDeferred()
        viewModel.onEvent(WithdrawUiEvent.WithdrawClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isWithdrawing)
        viewModel.onEvent(WithdrawUiEvent.WithdrawClicked)
        advanceUntilIdle()

        assertEquals(1, authRepository.withdrawCalls)
        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }

    @Test
    fun 탈퇴에_실패하면_오류를_알리고_화면에_남아_다시_시도할_수_있다() = runTest {
        val cause = IOException("연결 끊김")
        authRepository.withdrawResult = Result.failure(cause)

        viewModel.onEvent(WithdrawUiEvent.WithdrawClicked)

        assertEquals(WithdrawUiEffect.ShowWithdrawError(cause), viewModel.effect.first())
        assertFalse(viewModel.uiState.value.isWithdrawing)

        authRepository.withdrawResult = Result.success(Unit)
        viewModel.onEvent(WithdrawUiEvent.WithdrawClicked)
        assertEquals(WithdrawUiEffect.NavigateToLogin, viewModel.effect.first())
        assertEquals(2, authRepository.withdrawCalls)
    }

    @Test
    fun 뒤로가기를_누르면_돌아간다() = runTest {
        viewModel.onEvent(WithdrawUiEvent.BackClicked)

        assertEquals(WithdrawUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 탈퇴_요청_중에는_뒤로가지_않는다() = runTest {
        authRepository.withdrawGate = CompletableDeferred()
        viewModel.onEvent(WithdrawUiEvent.WithdrawClicked)
        advanceUntilIdle()

        viewModel.onEvent(WithdrawUiEvent.BackClicked)

        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }
}
