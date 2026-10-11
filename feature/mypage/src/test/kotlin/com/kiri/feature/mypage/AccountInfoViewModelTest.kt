package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.account.AccountInfoUiEffect
import com.kiri.feature.mypage.model.account.AccountInfoUiEvent
import kotlinx.coroutines.flow.first
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

    private val viewModel = AccountInfoViewModel()

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
    fun 팝업에서_확인하면_팝업을_닫고_로그인_화면으로_간다() = runTest {
        viewModel.onEvent(AccountInfoUiEvent.LogoutClicked)
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)

        assertFalse(viewModel.uiState.value.isLogoutDialogVisible)
        assertEquals(AccountInfoUiEffect.NavigateToLogin, viewModel.effect.first())
    }

    @Test
    fun 팝업이_없을_때_온_확인은_무시한다() = runTest {
        viewModel.onEvent(AccountInfoUiEvent.LogoutConfirmed)

        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }
}
