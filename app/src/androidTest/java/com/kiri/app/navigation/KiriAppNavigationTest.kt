package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.app.NavigationTestActivity
import com.kiri.uikit.KiriTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KiriAppNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<NavigationTestActivity>()

    private fun start() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openMission() {
        compose.onNodeWithTag("home_question_card").performScrollTo().performTouchInput { swipeLeft() }
        compose.onNodeWithTag("home_mission_card").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("open_custom").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
    }

    @Test fun bottomTabsRemainVisibleOnDetail() {
        start(); openMission()
        listOf("home", "record", "place", "mypage").forEach { compose.onNodeWithTag("bottom_nav_$it").assertIsDisplayed() }
    }

    @Test fun detailBackStacksRemainIndependentAcrossTabs() {
        start(); openMission()
        compose.onNodeWithTag("bottom_nav_record").performClick()
        compose.onNodeWithText("상세보기").performClick()
        compose.onNodeWithText("기록 상세").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_home").performClick()
        compose.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_record").performClick()
        compose.onNodeWithText("기록 상세").assertIsDisplayed()
    }

    @Test fun backReturnsToHomeAndKeepsMissionSide() {
        start(); openMission()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("home_mission_card").assertIsDisplayed()
    }

    @Test fun customApplyReturnsToDetailAndRestoresSelectionOnReentry() {
        start(); openMission()
        compose.onNodeWithTag("open_custom").performClick()
        compose.onNodeWithTag("condition_MORE_REST").performScrollTo().performClick()
        compose.onNodeWithTag("apply_custom").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("condition_custom_screen").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
        compose.onNodeWithTag("open_custom").performClick()
        compose.onNodeWithTag("condition_MORE_REST").assertIsOn()
        compose.onNodeWithTag("condition_LESS_WALKING").performClick()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("open_custom").performClick()
        compose.onNodeWithTag("condition_LESS_WALKING").assertIsOff()
    }
}
