package com.kiri.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// OnboardingRoute가 hiltViewModel()을 쓰므로 Hilt가 적용된 MainActivity에서 실행한다
@RunWith(AndroidJUnit4::class)
class KiriAppStartTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun passAuth() {
        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("다음").performClick()
        composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
    }

    @Test
    fun 앱을_실행하면_로그인_화면에서_시작한다() {
        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_screen").assertDoesNotExist()
    }

    @Test
    fun 로그인_후_온보딩_닉네임_입력으로_이동한다() {
        passAuth()

        composeTestRule.onNodeWithTag("input_nickname_screen").assertIsDisplayed()
    }

    @Test
    fun 온보딩_첫_화면에서_시스템_뒤로가기를_누르면_로그인으로_돌아간다() {
        passAuth()

        pressSystemBack()

        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_screen").assertDoesNotExist()
    }

    @Test
    fun 닉네임을_입력하고_역할을_선택하면_온보딩_메인으로_이동한다() {
        passAuth()
        composeTestRule.onNodeWithTag("onboarding_input_box").performTextInput("주연주")
        composeTestRule.onNodeWithText("다음").performClick()

        composeTestRule.onNodeWithText("주연주님의\n역할을 선택해주세요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("아빠").performClick()

        composeTestRule.onNodeWithTag("onboarding_main_screen").assertIsDisplayed()
    }

    @Test
    fun 온보딩_메인에서_뒤로가기_버튼을_누르면_역할_선택으로_돌아간다() {
        passAuth()
        composeTestRule.onNodeWithTag("onboarding_input_box").performTextInput("주연주")
        composeTestRule.onNodeWithText("다음").performClick()
        composeTestRule.onNodeWithText("아빠").performClick()

        composeTestRule.onNodeWithTag("onboarding_back_button").performClick()

        composeTestRule.onNodeWithTag("choose_role_screen").assertIsDisplayed()
    }

    @Test
    fun 시스템_뒤로가기를_누르면_이전_단계로_돌아가고_입력값이_유지된다() {
        passAuth()
        composeTestRule.onNodeWithTag("onboarding_input_box").performTextInput("주연주")
        composeTestRule.onNodeWithText("다음").performClick()
        composeTestRule.onNodeWithText("아빠").performClick()

        pressSystemBack()
        composeTestRule.onNodeWithTag("choose_role_screen").assertIsDisplayed()

        pressSystemBack()
        composeTestRule.onNodeWithTag("input_nickname_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("주연주").assertIsDisplayed()
    }

    private fun pressSystemBack() {
        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }
}
