package com.kiri.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kiri.app.HiltTestActivity
import com.kiri.data.home.HomeRepositoryModule
import com.kiri.domain.home.DailyQuestion
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.HomeException
import com.kiri.domain.home.HomeFailure
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.HomeTutorialRepository
import com.kiri.domain.home.NoHomeTutorial
import com.kiri.uikit.KiriTheme
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.LocalDate
import javax.inject.Singleton
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// 둘러보기 사용자(소속된 가족 없음)는 기록 탭과 마이페이지의 가족 설정 메뉴로 가지 못한다 (토스트로 안내)
// 실제 서버 대신 가족이 없다고 답하는 저장소를 쓴다
@HiltAndroidTest
@UninstallModules(HomeRepositoryModule::class)
class BrowsingAccessNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Before fun setUp() { hilt.inject() }

    @Test fun 둘러보기_중에는_기록_탭으로_가지_않는다() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitForIdle()

        compose.onNodeWithTag("bottom_nav_record").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("record_screen").assertDoesNotExist()
    }

    @Test fun 둘러보기_중에는_마이페이지의_가족_설정_메뉴로_가지_않는다() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.onNodeWithTag("bottom_nav_mypage").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("mypage_screen").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()

        listOf("family_setting", "home_address", "invite_link").forEach { menu ->
            compose.onNodeWithTag("mypage_menu_$menu").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("mypage_screen").assertIsDisplayed()
        }
        compose.onNodeWithTag("family_setting_screen").assertDoesNotExist()
        compose.onNodeWithTag("home_address_screen").assertDoesNotExist()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestModule {
        @Provides fun clock(): Clock = Clock.systemUTC()
        @Provides fun tutorial(): HomeTutorialRepository = NoHomeTutorial

        // 소속된 가족이 없는 사용자. 홈 전체는 이 테스트에서 쓰지 않는다
        @Provides @Singleton fun home(): HomeRepository = object : HomeRepository {
            private val browsing = FamilyHome(LocalDate.now(), "13", null, emptyList(), null, emptyMap(), emptySet(), null, serverBacked = true)
            override suspend fun load(): FamilyHome = browsing
            override suspend fun family(): FamilyHome = browsing
            override suspend fun saveAnswer(question: DailyQuestion, answer: String) = throw HomeException(HomeFailure.UNAVAILABLE)
            override suspend fun poke(question: DailyQuestion, memberId: String) = throw HomeException(HomeFailure.UNAVAILABLE)
        }
    }
}
