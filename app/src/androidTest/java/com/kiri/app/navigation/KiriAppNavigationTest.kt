package com.kiri.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.uikit.KiriTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO: 로그인 → 온보딩 완료 → 메인 이동이 연결되면 KiriApp 전체 흐름 테스트 복구
//  (로그인/온보딩을 거쳐 메인에 도달, 메인에서 뒤로가기 시 로그인/온보딩으로 돌아가지 않음)
@RunWith(AndroidJUnit4::class)
class KiriAppNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setMainContent() {
        composeTestRule.setContent { KiriTheme { MainScreen() } }
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun 메인_화면에_바텀바_탭이_모두_보인다() {
        setMainContent()

        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_record").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_place").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").assertIsDisplayed()
    }

    @Test
    fun 탭을_전환해도_각_탭의_디테일_화면이_독립적으로_유지된다() {
        setMainContent()

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
        setMainContent()

        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("홈 상세").assertIsDisplayed()

        composeTestRule.onNodeWithText("뒤로").performClick()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }
}
