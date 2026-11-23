package com.kiri.app.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.auth.presentation.SplashScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 스플래시는_로그인_버튼이_나타나는_시점까지_재생한_뒤_끝났다고_알린다() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            KiriTheme { SplashScreen(onFinished = { finished = true }) }
        }

        // 버튼이 나타나기 시작하는 시점은 약 2.9초
        composeTestRule.mainClock.advanceTimeBy(2_500)
        assertFalse(finished)

        composeTestRule.mainClock.advanceTimeBy(1_000)
        assertTrue(finished)
        composeTestRule.onNodeWithTag("splash_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_wordmark").assertIsDisplayed()
        // 스플래시에는 로그인 버튼이 없다
        composeTestRule.onNodeWithTag("auth_login_button_kakao").assertDoesNotExist()
    }

    @Test
    fun 재생을_건너뛰면_바로_끝났다고_알린다() {
        var finished = false
        composeTestRule.setContent {
            KiriTheme { SplashScreen(onFinished = { finished = true }, play = false) }
        }

        composeTestRule.waitForIdle()
        assertTrue(finished)
    }
}
