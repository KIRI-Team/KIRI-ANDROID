package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kiri.app.HiltTestActivity
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PlaceMissionNavigationTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @Before fun setUp() { hiltRule.inject() }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    @Test fun riverMissionOpensRecommendationsReviewsRestoreAndBackReturnsToFeed() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.onNodeWithTag("bottom_nav_place").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("place_card_place-river").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("place_card_place-river").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("preview_activity_tab").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mission_preview_screen").assertIsDisplayed()
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기").assertIsDisplayed()
        compose.onNodeWithTag("preview_activity_tab").assertIsSelected()
        compose.onNodeWithTag("preview_review_tab").performScrollTo().performClick()
        compose.onNodeWithTag("mission_review_river-review-0").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_home").performClick()
        compose.onNodeWithTag("bottom_nav_place").performClick()
        compose.onNodeWithTag("preview_review_tab").assertIsSelected()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("place_screen").assertIsDisplayed()
        compose.onNodeWithTag("place_card_place-river").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("preview_activity_tab").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("preview_activity_tab").assertIsSelected()
    }
}
