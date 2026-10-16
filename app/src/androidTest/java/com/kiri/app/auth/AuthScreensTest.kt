package com.kiri.app.auth

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.auth.AuthViewModel
import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.auth.login.NonceGenerator
import com.kiri.feature.auth.login.SocialLoginResult
import com.kiri.feature.auth.model.AuthUiEvent
import com.kiri.feature.auth.model.LoginProvider
import com.kiri.feature.auth.presentation.AuthScreen
import com.kiri.feature.auth.presentation.LoginScreen
import com.kiri.feature.auth.presentation.TermsAgreementScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 로그인을 마치고 약관동의에 온 ViewModel (SDK·서버 대신 가짜 결과를 넣는다)
    private fun loggedInViewModel() = AuthViewModel(FakeAuthRepository(), NonceGenerator()).apply {
        onEvent(AuthUiEvent.LoginClicked(LoginProvider.KAKAO))
        onEvent(AuthUiEvent.SocialLoginFinished(SocialLoginResult.Success(LoginProvider.KAKAO, "test-id-token")))
    }

    @Test
    fun 로그인_화면에는_카카오와_구글_로그인_버튼만_있다() {
        val clicked = mutableListOf<LoginProvider>()
        composeTestRule.setContent {
            KiriTheme { LoginScreen(onLoginClick = { clicked += it }) }
        }

        composeTestRule.onNodeWithText("카카오 로그인").performClick()
        composeTestRule.onNodeWithText("Google로 로그인").performClick()
        composeTestRule.onNodeWithText("Apple로 로그인").assertDoesNotExist()

        assertEquals(listOf(LoginProvider.KAKAO, LoginProvider.GOOGLE), clicked)
    }

    @Test
    fun 전체동의를_누르면_모든_약관이_체크되고_계속하기가_활성화된다() {
        composeTestRule.setContent {
            var agreed by remember { mutableStateOf(emptySet<AgreementTerm>()) }
            KiriTheme {
                TermsAgreementScreen(
                    agreedTerms = agreed,
                    allAgreed = agreed.containsAll(AgreementTerm.entries),
                    canContinue = agreed.containsAll(AgreementTerm.entries),
                    onAllToggle = {
                        agreed = if (agreed.containsAll(AgreementTerm.entries)) emptySet() else AgreementTerm.entries.toSet()
                    },
                    onTermToggle = { agreed = if (it in agreed) agreed - it else agreed + it },
                    onTermDetailClick = {},
                    onAgreeClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("서비스 이용을 위해\n동의가 필요해요").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("agreement_term_1_checkbox").assertIsOff()

        composeTestRule.onNodeWithTag("agreement_all").performClick()

        AgreementTerm.entries.forEach {
            composeTestRule.onNodeWithTag("agreement_${it.name.lowercase()}_checkbox").assertIsOn()
        }
        composeTestRule.onNodeWithTag("kiri_button").assertIsEnabled()
    }

    @Test
    fun 개별_약관은_화살표만_세부_내용을_열고_나머지_영역은_동의를_바꾼다() {
        val toggled = mutableListOf<AgreementTerm>()
        val detail = mutableListOf<AgreementTerm>()
        composeTestRule.setContent {
            KiriTheme {
                TermsAgreementScreen(
                    agreedTerms = emptySet(),
                    allAgreed = false,
                    canContinue = false,
                    onAllToggle = {},
                    onTermToggle = { toggled += it },
                    onTermDetailClick = { detail += it },
                    onAgreeClick = {},
                    onBackClick = {},
                )
            }
        }

        // 동의 영역의 가운데(약관 문구)와 왼쪽 끝(체크박스)을 누른다
        composeTestRule.onNodeWithTag("agreement_term_2_checkbox").performClick()
        composeTestRule.onNodeWithTag("agreement_term_1_checkbox").performTouchInput { click(centerLeft + Offset(40f, 0f)) }
        composeTestRule.onNodeWithTag("agreement_term_3_detail").performClick()

        assertEquals(listOf(AgreementTerm.TERM_2, AgreementTerm.TERM_1), toggled)
        assertEquals(listOf(AgreementTerm.TERM_3), detail)
    }

    @Test
    fun 화살표를_누르면_세부_내용_창이_열리고_동의는_바뀌지_않는다() {
        composeTestRule.setContent {
            val viewModel = remember { loggedInViewModel() }
            val state by viewModel.uiState.collectAsState()
            KiriTheme { AuthScreen(state = state, onEvent = viewModel::onEvent) }
        }
        composeTestRule.onNodeWithTag("term_detail_sheet").assertDoesNotExist()

        composeTestRule.onNodeWithTag("agreement_term_1_detail").performClick()

        composeTestRule.onNodeWithTag("term_detail_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("제1조 (목적)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("agreement_term_1_checkbox").assertIsOff()
    }

    @Test
    fun 세부_내용_창의_동의하기를_누르면_창이_닫히고_해당_약관이_체크된다() {
        composeTestRule.setContent {
            val viewModel = remember { loggedInViewModel() }
            val state by viewModel.uiState.collectAsState()
            KiriTheme { AuthScreen(state = state, onEvent = viewModel::onEvent) }
        }
        composeTestRule.onNodeWithTag("agreement_term_2_detail").performClick()

        composeTestRule.onNodeWithTag("term_detail_agree_button").performClick()

        composeTestRule.onNodeWithTag("term_detail_sheet").assertDoesNotExist()
        composeTestRule.onNodeWithTag("agreement_term_2_checkbox").assertIsOn()
        composeTestRule.onNodeWithTag("agreement_term_1_checkbox").assertIsOff()
    }
}
