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
import com.kiri.domain.model.term.Term
import com.kiri.feature.auth.AuthViewModel
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
    private fun loggedInViewModel() = AuthViewModel(FakeAuthRepository(), FakeTermRepository(), NonceGenerator()).apply {
        onEvent(AuthUiEvent.LoginClicked(LoginProvider.KAKAO))
        onEvent(AuthUiEvent.SocialLoginFinished(SocialLoginResult.Success(LoginProvider.KAKAO, "test-id-token")))
    }

    private fun setTermsAgreementScreen(
        isLoadingTerms: Boolean = false,
        termsLoadFailed: Boolean = false,
        onRetryClick: () -> Unit = {},
        onTermToggle: (Term) -> Unit = {},
        onTermDetailClick: (Term) -> Unit = {},
    ) {
        composeTestRule.setContent {
            KiriTheme {
                TermsAgreementScreen(
                    terms = if (isLoadingTerms || termsLoadFailed) emptyList() else FAKE_TERMS,
                    agreedTermIds = emptySet(),
                    allAgreed = false,
                    canContinue = false,
                    onAllToggle = {},
                    onTermToggle = onTermToggle,
                    onTermDetailClick = onTermDetailClick,
                    onAgreeClick = {},
                    onBackClick = {},
                    isLoadingTerms = isLoadingTerms,
                    termsLoadFailed = termsLoadFailed,
                    onRetryClick = onRetryClick,
                )
            }
        }
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
    fun 서버_약관마다_필수와_선택_표시가_붙은_제목이_보인다() {
        setTermsAgreementScreen()

        composeTestRule.onNodeWithText("[필수] 서비스 이용약관").assertIsDisplayed()
        composeTestRule.onNodeWithText("[필수] 개인정보 처리방침").assertIsDisplayed()
        composeTestRule.onNodeWithText("[선택] 마케팅 정보 수신 동의").assertIsDisplayed()
    }

    @Test
    fun 전체동의를_누르면_모든_약관이_체크되고_계속하기가_활성화된다() {
        composeTestRule.setContent {
            var agreed by remember { mutableStateOf(emptySet<Long>()) }
            val allIds = FAKE_TERMS.map(Term::id).toSet()
            KiriTheme {
                TermsAgreementScreen(
                    terms = FAKE_TERMS,
                    agreedTermIds = agreed,
                    allAgreed = agreed.containsAll(allIds),
                    canContinue = agreed.containsAll(allIds),
                    onAllToggle = { agreed = if (agreed.containsAll(allIds)) emptySet() else allIds },
                    onTermToggle = { agreed = if (it.id in agreed) agreed - it.id else agreed + it.id },
                    onTermDetailClick = {},
                    onAgreeClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("서비스 이용을 위해\n동의가 필요해요").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("agreement_term_1_checkbox").assertIsOff()
        // 동의하지 않은 약관은 빈 원으로 보여준다
        composeTestRule.onNodeWithTag("agreement_term_1_unchecked", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("agreement_all").performClick()

        FAKE_TERMS.forEach {
            composeTestRule.onNodeWithTag("agreement_term_${it.id}_checkbox").assertIsOn()
            composeTestRule.onNodeWithTag("agreement_term_${it.id}_unchecked", useUnmergedTree = true).assertDoesNotExist()
        }
        composeTestRule.onNodeWithTag("kiri_button").assertIsEnabled()
    }

    @Test
    fun 개별_약관은_화살표만_세부_내용을_열고_나머지_영역은_동의를_바꾼다() {
        val toggled = mutableListOf<Term>()
        val detail = mutableListOf<Term>()
        setTermsAgreementScreen(onTermToggle = { toggled += it }, onTermDetailClick = { detail += it })

        // 동의 영역의 가운데(약관 문구)와 왼쪽 끝(체크박스)을 누른다
        composeTestRule.onNodeWithTag("agreement_term_2_checkbox").performClick()
        composeTestRule.onNodeWithTag("agreement_term_1_checkbox").performTouchInput { click(centerLeft + Offset(40f, 0f)) }
        composeTestRule.onNodeWithTag("agreement_term_3_detail").performClick()

        assertEquals(listOf(FAKE_TERMS[1], FAKE_TERMS[0]), toggled)
        assertEquals(listOf(FAKE_TERMS[2]), detail)
    }

    @Test
    fun 약관을_받는_동안에는_로딩만_보이고_동의하기를_누를_수_없다() {
        setTermsAgreementScreen(isLoadingTerms = true)

        composeTestRule.onNodeWithTag("kiri_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agreement_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("kiri_button").assertIsNotEnabled()
    }

    @Test
    fun 약관을_받지_못하면_다시_시도를_누를_수_있다() {
        var retried = 0
        setTermsAgreementScreen(termsLoadFailed = true, onRetryClick = { retried++ })

        composeTestRule.onNodeWithTag("terms_load_failed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agreement_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("terms_retry_button").performClick()

        assertEquals(1, retried)
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
