package com.kiri.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// AuthRoute, OnboardingRoute가 hiltViewModel()을 쓰므로 Hilt가 적용된 MainActivity에서 실행한다
// 테스트는 HiltTestApplication으로 실행되므로 @HiltAndroidTest와 HiltAndroidRule이 필요하다
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KiriAppStartTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // 로그인 → 약관 전체동의 → 동의하고 계속하기 → 온보딩
    private fun passAuth() {
        composeTestRule.onNodeWithTag("login_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_login_button_kakao").performClick()
        composeTestRule.onNodeWithTag("terms_agreement_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agreement_all").performClick()
        composeTestRule.onNodeWithText("동의하고 계속하기").performClick()
        composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
    }

    @Test
    fun 앱을_실행하면_로그인_화면에서_시작한다() {
        composeTestRule.onNodeWithTag("login_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_screen").assertDoesNotExist()
    }

    @Test
    fun 약관에_모두_동의하기_전에는_계속할_수_없다() {
        composeTestRule.onNodeWithTag("auth_login_button_kakao").performClick()

        composeTestRule.onNodeWithText("동의하고 계속하기").performClick()

        composeTestRule.onNodeWithTag("terms_agreement_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_screen").assertDoesNotExist()
    }

    @Test
    fun 약관동의에서_뒤로가기를_누르면_로그인_화면으로_돌아간다() {
        composeTestRule.onNodeWithTag("auth_login_button_kakao").performClick()
        composeTestRule.onNodeWithTag("terms_agreement_screen").assertIsDisplayed()

        pressSystemBack()

        composeTestRule.onNodeWithTag("login_screen").assertIsDisplayed()
    }

    @Test
    fun 로그인_후_온보딩_닉네임_입력으로_이동한다() {
        passAuth()

        composeTestRule.onNodeWithTag("input_nickname_screen").assertIsDisplayed()
    }

    @Test
    fun 온보딩_첫_화면에서_시스템_뒤로가기를_누르면_약관동의로_돌아간다() {
        passAuth()

        pressSystemBack()

        composeTestRule.onNodeWithTag("terms_agreement_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_screen").assertDoesNotExist()
    }

    @Test
    fun 닉네임을_입력하고_역할을_선택한_뒤_다음을_누르면_온보딩_메인으로_이동한다() {
        passAuth()
        composeTestRule.onNodeWithTag("onboarding_input_box").performTextInput("주연주")
        composeTestRule.onNodeWithText("다음").performClick()

        composeTestRule.onNodeWithText("주연주님의\n역할을 선택해주세요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("아빠").performClick()
        composeTestRule.onNodeWithTag("choose_role_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("다음").performClick()

        composeTestRule.onNodeWithTag("onboarding_main_screen").assertIsDisplayed()
    }

    @Test
    fun 온보딩_메인에서_뒤로가기_버튼을_누르면_역할_선택으로_돌아간다() {
        passAuth()
        passNicknameAndRole()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        composeTestRule.onNodeWithTag("choose_role_screen").assertIsDisplayed()
    }

    @Test
    fun 시스템_뒤로가기를_누르면_이전_단계로_돌아가고_입력값이_유지된다() {
        passAuth()
        passNicknameAndRole()

        pressSystemBack()
        composeTestRule.onNodeWithTag("choose_role_screen").assertIsDisplayed()

        pressSystemBack()
        composeTestRule.onNodeWithTag("input_nickname_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("주연주").assertIsDisplayed()
    }

    @Test
    fun 온보딩_메인에서_가족공간_만들기를_누르면_가족명_입력으로_이동한다() {
        passAuth()
        passNicknameAndRole()

        composeTestRule.onNodeWithText("가족공간 만들러 가기").performClick()

        composeTestRule.onNodeWithTag("input_family_name_screen").assertIsDisplayed()
    }

    @Test
    fun 온보딩_메인에서_가족코드_입력하기를_누르면_가족코드_입력으로_이동한다() {
        passAuth()
        passNicknameAndRole()

        composeTestRule.onNodeWithText("가족코드 입력하기").performClick()

        composeTestRule.onNodeWithTag("input_family_code_screen").assertIsDisplayed()
    }

    // 닉네임 입력 → 다음 → 역할 선택 → 다음 → 온보딩 메인
    private fun passNicknameAndRole() {
        composeTestRule.onNodeWithTag("onboarding_input_box").performTextInput("주연주")
        composeTestRule.onNodeWithText("다음").performClick()
        composeTestRule.onNodeWithText("아빠").performClick()
        composeTestRule.onNodeWithText("다음").performClick()
    }

    private fun pressSystemBack() {
        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }
}
