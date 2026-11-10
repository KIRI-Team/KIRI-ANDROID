package com.kiri.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kiri.app.HiltTestActivity
import com.kiri.data.home.HomeRepositoryModule
import com.kiri.domain.home.DailyQuestion
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.FamilyMember
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

// 마이페이지 프로필과 가족 기본 설정이 가족 정보로 채워지고, 수정 아이콘으로 가족명·닉네임 수정에 가는지 확인한다
// 실제 서버 대신 가족 정보만 돌려주는 저장소를 쓴다 (홈 화면은 쓰지 않는다)
@HiltAndroidTest
@UninstallModules(HomeRepositoryModule::class)
class MypageFamilyNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Before fun setUp() { hilt.inject() }

    private fun openFamilySetting() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.onNodeWithTag("bottom_nav_mypage").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("mypage_screen").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun 마이페이지_프로필에_내_닉네임과_가족_이름_역할이_보인다() {
        openFamilySetting()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("쥬123").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("끼리네가족 I 딸").assertIsDisplayed()
    }

    @Test fun 가족_기본_설정에서_가족명_수정_화면으로_이동하고_뒤로가면_돌아온다() {
        openFamilySetting()
        compose.onNodeWithTag("mypage_menu_family_setting").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("family_name_edit").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("family_name_edit").performClick()
        compose.onNodeWithTag("family_name_edit_screen").assertIsDisplayed()
        compose.onNodeWithText("지금 가족명: 끼리네가족").assertIsDisplayed()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("family_setting_screen").assertIsDisplayed()
    }

    @Test fun 가족_기본_설정에서_내_카드의_수정_아이콘을_누르면_닉네임_수정_화면으로_이동한다() {
        openFamilySetting()
        compose.onNodeWithTag("mypage_menu_family_setting").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("family_member_nickname_edit").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("family_member_nickname_edit").performClick()
        compose.onNodeWithTag("nickname_edit_screen").assertIsDisplayed()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestModule {
        @Provides fun clock(): Clock = Clock.systemUTC()
        @Provides fun tutorial(): HomeTutorialRepository = NoHomeTutorial

        // 가족 정보만 돌려준다. 홈 전체는 이 테스트에서 쓰지 않으므로 불러오지 못한 것으로 둔다
        @Provides @Singleton fun home(): HomeRepository = object : HomeRepository {
            private val family = FamilyHome(
                LocalDate.now(), "1", "끼리네가족",
                listOf(FamilyMember("1", "쥬123", "딸"), FamilyMember("2", "마마", "엄마")),
                null, emptyMap(), emptySet(), null, serverBacked = true, familyId = "9",
            )
            override suspend fun load(): FamilyHome = throw HomeException(HomeFailure.UNAVAILABLE)
            override suspend fun family(): FamilyHome = family
            override suspend fun saveAnswer(question: DailyQuestion, answer: String) = throw HomeException(HomeFailure.UNAVAILABLE)
            override suspend fun poke(question: DailyQuestion, memberId: String) = throw HomeException(HomeFailure.UNAVAILABLE)
        }
    }
}
