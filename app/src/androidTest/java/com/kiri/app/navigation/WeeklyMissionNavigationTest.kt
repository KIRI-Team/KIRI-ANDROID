package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kiri.app.HiltTestActivity
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.home.HomeRepositoryModule
import com.kiri.domain.home.repository.*
import com.kiri.uikit.KiriTheme
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.*
import dagger.hilt.components.SingletonComponent
import java.time.*
import java.util.UUID
import org.junit.*

@HiltAndroidTest
@UninstallModules(HomeRepositoryModule::class)
class WeeklyMissionNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    private var hasWeeklyMission = true
    private val homeSource = FakeHomeRepository(Clock.systemUTC(), latencyMillis = 0)
    private val identity = UUID.randomUUID().toString()

    @BindValue @JvmField
    val home: HomeRepository = object : HomeRepository by homeSource {
        override suspend fun load(): FamilyHome {
            val loaded = homeSource.load()
            return loaded.copy(
                familyName = "주간 미션 테스트 $identity",
                schedules = emptyList(),
                mission = loaded.mission?.copy(id = identity)?.takeIf { hasWeeklyMission },
            )
        }

        override suspend fun family(): FamilyHome = load()
    }
    @Before fun setup() { hilt.inject() }

    @Test fun answerEditorHidesBottomNavigationAndBackRestoresIt() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("home_mission_card").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("질문 카드로 전환").performScrollTo().performClick()
        compose.onNodeWithTag("home_question_card").performScrollTo().performClick()
        compose.onNodeWithTag("bottom_nav_home").assertDoesNotExist()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("bottom_nav_home").assertIsSelected()
    }

    @Test fun emptyMissionLinkOpensPlaceAndKeepsBottomTabsUsable() {
        hasWeeklyMission = false
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("home_mission_card").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("공개된 미션을 둘러보세요 →").performScrollTo().performClick()
        compose.onNodeWithTag("bottom_nav_place").assertIsSelected()
        compose.onNodeWithTag("place_screen").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_mypage").performClick().assertIsSelected()
        compose.onNodeWithTag("bottom_nav_home").performClick().assertIsSelected()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestModule {
        @Provides fun clock(): Clock = Clock.systemUTC()
        @Provides fun tutorial(): HomeTutorialRepository = NoHomeTutorial
    }
}
