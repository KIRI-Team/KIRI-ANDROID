package com.kiri.app.mypage

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.model.account.AccountInfoUiEvent
import com.kiri.feature.mypage.model.account.AccountInfoUiState
import com.kiri.feature.mypage.presentation.account.AccountInfoScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountInfoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<AccountInfoUiEvent>()

    private fun setContent(state: AccountInfoUiState = AccountInfoUiState()) {
        composeTestRule.setContent {
            KiriTheme { AccountInfoScreen(state = state, onEvent = { events += it }, appVersion = "1.0.0") }
        }
    }

    @Test
    fun 메뉴와_앱_버전이_보이고_앱_버전은_눌리지_않는다() {
        setContent()

        composeTestRule.onNodeWithText("계정 정보").assertIsDisplayed()
        listOf("이용 약관", "오픈 라이선스", "앱 버전", "로그아웃", "회원탈퇴").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("1.0.0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_info_app_version").assertHasNoClickAction()
        composeTestRule.onNodeWithTag("account_info_logout").assertHasClickAction()
        composeTestRule.onNodeWithTag("logout_dialog").assertDoesNotExist()
    }

    @Test
    fun 각_메뉴와_뒤로가기를_누르면_이벤트를_전달한다() {
        setContent()

        listOf("terms", "open_license", "logout", "withdraw").forEach {
            composeTestRule.onNodeWithTag("account_info_$it").performClick()
        }
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(
            listOf(
                AccountInfoUiEvent.TermsClicked,
                AccountInfoUiEvent.OpenLicenseClicked,
                AccountInfoUiEvent.LogoutClicked,
                AccountInfoUiEvent.WithdrawClicked,
                AccountInfoUiEvent.BackClicked,
            ),
            events,
        )
    }

    @Test
    fun 로그아웃_팝업의_문구가_보이고_취소와_확인을_전달한다() {
        setContent(AccountInfoUiState(isLogoutDialogVisible = true))

        composeTestRule.onNodeWithTag("logout_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("정말로 로그아웃 하시겠어요?").assertIsDisplayed()
        composeTestRule.onNodeWithText("다시 로그인하면 우리 가족의 기록을 이어갈 수 있어요.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("logout_dialog_cancel").performClick()
        composeTestRule.onNodeWithTag("logout_dialog_confirm").performClick()

        assertEquals(listOf(AccountInfoUiEvent.LogoutCanceled, AccountInfoUiEvent.LogoutConfirmed), events)
    }
}
