package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kiri.app.NavigationTestActivity
import com.kiri.uikit.KiriTheme
import org.junit.Rule
import org.junit.Test

class PlaceMissionNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<NavigationTestActivity>()
    @Test fun riverMissionOpensRecommendationsReviewsRestoreAndBackReturnsToFeed() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.onNodeWithTag("bottom_nav_place").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("place_card_place-river").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("place_card_place-river").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("preview_activity_tab").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mission_preview_screen").assertIsDisplayed()
        compose.onNodeWithText("한강공원에서 산책하기").assertIsDisplayed()
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
