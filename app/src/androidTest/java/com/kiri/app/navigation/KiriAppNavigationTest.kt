package com.kiri.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.uikit.KiriTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KiriAppNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContentAndCompleteOnboarding() {
        composeTestRule.setContent { KiriTheme { KiriApp() } }
        composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_input_box").performTextInput("주연주")
        composeTestRule.onNodeWithText("다음").performClick()
        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("다음").performClick()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun 온보딩과_로그인을_거쳐야_메인_바텀바에_도달한다() {
        setContentAndCompleteOnboarding()

        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_record").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_place").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").assertIsDisplayed()
    }

    @Test
    fun 메인_진입_후_뒤로가기를_눌러도_온보딩이나_로그인으로_돌아가지_않는다() {
        setContentAndCompleteOnboarding()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("auth_screen").assertDoesNotExist()
    }

    @Test
    fun 탭을_전환해도_각_탭의_디테일_화면이_독립적으로_유지된다() {
        setContentAndCompleteOnboarding()

        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("홈 상세").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()
        composeTestRule.onNodeWithTag("record_screen").assertIsDisplayed()

        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("기록 상세").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_home").performClick()
        composeTestRule.onNodeWithText("홈 상세").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()
        composeTestRule.onNodeWithText("기록 상세").assertIsDisplayed()
    }

    @Test
    fun 상세화면에서_뒤로가기_버튼을_누르면_해당_탭의_시작화면으로_돌아간다() {
        setContentAndCompleteOnboarding()

        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("홈 상세").assertIsDisplayed()

        composeTestRule.onNodeWithText("뒤로").performClick()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }
}
