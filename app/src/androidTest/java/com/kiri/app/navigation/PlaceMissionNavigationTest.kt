package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kiri.app.HiltTestActivity
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.home.HomeRepositoryModule
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.mission.MissionRepositoryModule
import com.kiri.data.place.FakePlaceRepository
import com.kiri.data.place.PlaceRepositoryModule
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.HomeTutorialRepository
import com.kiri.domain.home.NoHomeTutorial
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.place.PlaceRepository
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Clock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(PlaceRepositoryModule::class, MissionRepositoryModule::class, HomeRepositoryModule::class)
class PlaceMissionNavigationTest {
    @BindValue @JvmField val clock: Clock = Clock.systemUTC()
    @BindValue @JvmField val home: HomeRepository = FakeHomeRepository(clock, latencyMillis = 0)
    @BindValue @JvmField val tutorial: HomeTutorialRepository = NoHomeTutorial
    @BindValue @JvmField val missions: MissionRepository = FakeMissionRepository()
    @BindValue @JvmField val places: PlaceRepository = FakePlaceRepository(missionRepository = missions)

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
