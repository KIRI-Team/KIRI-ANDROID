package com.kiri.app.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.auth.model.LoginProvider
import com.kiri.feature.auth.presentation.LoginScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 애니메이션_중에는_로그인_버튼을_누를_수_없고_끝나면_누를_수_있다() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            KiriTheme { LoginScreen(onLoginClick = {}, onIntroFinished = { finished = true }) }
        }

        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithTag("auth_login_button_kakao").assertIsNotEnabled()

        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onNodeWithTag("auth_login_button_kakao").assertIsEnabled()
        composeTestRule.onNodeWithTag("login_wordmark").assertIsDisplayed()
        assertTrue(finished)
    }

    @Test
    fun 애니메이션을_건너뛰면_바로_버튼을_눌러_로그인_방식을_전달한다() {
        val clicked = mutableListOf<LoginProvider>()
        composeTestRule.setContent {
            KiriTheme { LoginScreen(onLoginClick = { clicked += it }, playIntro = false) }
        }

        composeTestRule.onNodeWithTag("auth_login_button_kakao").assertIsEnabled().performClick()
        composeTestRule.onNodeWithTag("auth_login_button_google").performClick()

        assertEquals(listOf(LoginProvider.KAKAO, LoginProvider.GOOGLE), clicked)
    }

    @Test
    fun 로그인이_진행_중이면_버튼을_누를_수_없다() {
        composeTestRule.setContent {
            KiriTheme { LoginScreen(onLoginClick = {}, loginEnabled = false, playIntro = false) }
        }

        composeTestRule.onNodeWithTag("auth_login_button_kakao").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("auth_login_button_google").assertIsNotEnabled()
    }

    @Test
    fun 스플래시에서_이어지면_처음부터_재생하지_않고_곧_버튼을_누를_수_있다() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            KiriTheme { LoginScreen(onLoginClick = {}, continueFromSplash = true, onIntroFinished = { finished = true }) }
        }

        // 처음부터 재생하면 1초 뒤에도 버튼이 막혀 있지만, 이어서 재생하면 나머지(약 0.9초)만 남는다
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithTag("auth_login_button_kakao").assertIsEnabled()
        assertTrue(finished)
    }
}
