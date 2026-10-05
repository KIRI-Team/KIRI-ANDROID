package com.kiri.app.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.auth.model.AgreementTerm
import com.kiri.feature.auth.model.LoginProvider
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
    fun 개별_약관의_체크박스와_문구는_각각_체크와_상세보기를_호출한다() {
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

        composeTestRule.onNodeWithTag("agreement_term_2_checkbox").performClick()
        composeTestRule.onNodeWithTag("agreement_term_3_detail").performClick()

        assertEquals(listOf(AgreementTerm.TERM_2), toggled)
        assertEquals(listOf(AgreementTerm.TERM_3), detail)
    }
}
