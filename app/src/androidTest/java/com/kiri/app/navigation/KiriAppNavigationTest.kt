package com.kiri.app.navigation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.app.HiltTestActivity
import com.kiri.feature.onboarding.TemporaryFamilyCode
import com.kiri.uikit.KiriTheme
import androidx.compose.ui.semantics.SemanticsProperties
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KiriAppNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Before fun setUp() { hiltRule.inject() }

    private var loginRequested = false

    private fun start() {
        compose.setContent { KiriTheme { MainScreen(onNavigateToLogin = { loginRequested = true }) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
    }

    // 홈 카드는 데이터를 불러온 뒤에 그려지므로 질문 카드가 나타날 때까지 기다린다
    private fun startOnHome() {
        setMainContent()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openMission() {
        composeTestRule.onNodeWithTag("home_question_card").performScrollTo().performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithTag("home_mission_card").performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag("open_custom").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
    }

    @Test
    fun homeInvitationShowsOnboardingTemporaryCodeAndReturnsHome() {
        startOnHome()
        composeTestRule.onNodeWithText("가족 초대하기").performScrollTo().performClick()
        composeTestRule.onNodeWithText(TemporaryFamilyCode.code).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로").performClick()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun tabSwitchRestoresSavedCustomConditions() {
        startOnHome()
        openMission()
        composeTestRule.onNodeWithTag("open_custom").performClick()
        composeTestRule.onNodeWithTag("condition_AVOID_CROWDS").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("apply_custom").performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag("condition_custom_screen").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()
        composeTestRule.onNodeWithTag("bottom_nav_home").performClick()
        composeTestRule.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_custom").performClick()
        composeTestRule.onNodeWithTag("condition_AVOID_CROWDS").assertIsOn()
    }

    @Test
    fun bottomTabsRemainVisibleOnDetail() {
        startOnHome()
        openMission()
        listOf("home", "record", "place", "mypage").forEach {
            composeTestRule.onNodeWithTag("bottom_nav_$it").assertIsDisplayed()
        }
    }

    @Test
    fun detailBackStacksRemainIndependentAcrossTabs() {
        startOnHome()
        openMission()
        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()
        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("기록 상세").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_home").performClick()
        composeTestRule.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()
        composeTestRule.onNodeWithText("기록 상세").assertIsDisplayed()
    }

    @Test
    fun backReturnsToHomeAndKeepsMissionSide() {
        startOnHome()
        openMission()
        composeTestRule.onNodeWithContentDescription("뒤로").performClick()
        composeTestRule.onNodeWithTag("home_mission_card").assertIsDisplayed()
    }

    @Test
    fun customApplyReturnsToDetailAndRestoresSelectionOnReentry() {
        startOnHome()
        openMission()
        composeTestRule.onNodeWithTag("open_custom").performClick()
        composeTestRule.onNodeWithTag("condition_MORE_REST").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("apply_custom").performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag("condition_custom_screen").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_custom").performClick()
        composeTestRule.onNodeWithTag("condition_MORE_REST").assertIsOn()
        composeTestRule.onNodeWithTag("condition_LESS_WALKING").performClick()
        composeTestRule.onNodeWithContentDescription("뒤로").performClick()
        composeTestRule.onNodeWithTag("open_custom").performClick()
        composeTestRule.onNodeWithTag("condition_LESS_WALKING").assertIsOff()
    }

    @Test fun homeInvitationShowsOnboardingTemporaryCodeAndReturnsHome() {
        start()
        compose.onNodeWithText("가족 초대하기").performScrollTo().performClick()
        compose.onNodeWithText(com.kiri.feature.onboarding.TemporaryFamilyCode.code).assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test fun thirdTabSavesAndRestoresBookmarkAcrossTabs() {
        start()
        compose.onNodeWithTag("bottom_nav_place").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("place_save_place-market").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("place_save_place-market").performClick()
        compose.onNodeWithTag("place_confirm_save").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("place_saved_toast").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("바로가기").performClick()
        compose.onNodeWithText("저장한 활동 1개").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_home").performClick()
        compose.onNodeWithTag("bottom_nav_place").performClick()
        compose.onNodeWithContentDescription("저장됨").assertIsDisplayed()
    }
    @Test fun tabSwitchRestoresSavedCustomConditions() {
        start(); openMission()
        compose.onNodeWithTag("open_custom").performClick()
        compose.onNodeWithTag("condition_AVOID_CROWDS").performScrollTo().performClick()
        compose.onNodeWithTag("apply_custom").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("condition_custom_screen").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("bottom_nav_record").performClick()
        compose.onNodeWithTag("bottom_nav_home").performClick()
        compose.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
        compose.onNodeWithTag("open_custom").performClick()
        compose.onNodeWithTag("condition_AVOID_CROWDS").assertIsOn()
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
    @Test
    fun 상세화면에서_뒤로가기_버튼을_누르면_해당_탭의_시작화면으로_돌아간다() {
        start()
        compose.onNodeWithTag("bottom_nav_record").performClick()

        compose.onNodeWithText("상세보기").performClick()
        compose.onNodeWithText("기록 상세").assertIsDisplayed()

        compose.onNodeWithText("뒤로").performClick()
        compose.onNodeWithTag("record_screen").assertIsDisplayed()
    }

    @Test
    fun 마이페이지_메뉴를_누르면_해당_화면으로_이동하고_뒤로가면_마이페이지로_돌아온다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()
        compose.onNodeWithTag("mypage_screen").assertIsDisplayed()

        compose.onNodeWithTag("mypage_menu_notification_setting").performClick()
        compose.onNodeWithTag("notification_setting_screen").assertIsDisplayed()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    @Test
    fun 우리집_주소_설정을_누르면_주소_화면으로_이동하고_뒤로가면_마이페이지로_돌아온다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()

        compose.onNodeWithTag("mypage_menu_home_address").performScrollTo().performClick()
        compose.onNodeWithTag("home_address_screen").assertIsDisplayed()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    @Test
    fun 가족_기본_설정에서_가족명_수정_화면으로_이동하고_뒤로가면_돌아온다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()
        compose.onNodeWithTag("mypage_menu_family_setting").performScrollTo().performClick()
        compose.onNodeWithTag("family_setting_screen").assertIsDisplayed()

        compose.onNodeWithTag("family_name_edit").performClick()
        compose.onNodeWithTag("family_name_edit_screen").assertIsDisplayed()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("family_setting_screen").assertIsDisplayed()
    }

    @Test
    fun 가족_기본_설정에서_내_카드의_수정_아이콘을_누르면_닉네임_수정_화면으로_이동한다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()
        compose.onNodeWithTag("mypage_menu_family_setting").performScrollTo().performClick()

        compose.onNodeWithTag("family_member_nickname_edit").performClick()
        compose.onNodeWithTag("nickname_edit_screen").assertIsDisplayed()
    }

    @Test
    fun 계정_정보를_누르면_계정_정보_화면으로_이동하고_뒤로가면_마이페이지로_돌아온다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()

        compose.onNodeWithTag("mypage_menu_account_info").performScrollTo().performClick()
        compose.onNodeWithTag("account_info_screen").assertIsDisplayed()
        compose.onNodeWithText("앱 버전").assertIsDisplayed()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    @Test
    fun 건의함에서_내용을_적고_의견_보내기를_누르면_마이페이지로_돌아온다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()

        compose.onNodeWithTag("mypage_menu_suggestion").performScrollTo().performClick()
        compose.onNodeWithTag("suggestion_screen").assertIsDisplayed()

        // 내용이 비어 있으면 눌러도 그대로 남는다
        compose.onNodeWithTag("kiri_button").performClick()
        compose.onNodeWithTag("suggestion_screen").assertIsDisplayed()

        compose.onNodeWithTag("suggestion_content_input").performTextInput("알림이 늦게 와요")
        compose.onNodeWithText("9/500").assertIsDisplayed()
        compose.onNodeWithTag("kiri_button").performClick()
        compose.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    private fun openAccountInfo() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()
        compose.onNodeWithTag("mypage_menu_account_info").performScrollTo().performClick()
        compose.onNodeWithTag("account_info_screen").assertIsDisplayed()
    }

    @Test
    fun 이용_약관에서_약관을_누르면_세부_내용_창이_열리고_확인을_누르면_닫힌다() {
        openAccountInfo()

        compose.onNodeWithTag("account_info_terms").performClick()
        compose.onNodeWithTag("terms_screen").assertIsDisplayed()

        compose.onNodeWithTag("terms_item_term_1").performClick()
        compose.onNodeWithTag("terms_detail_sheet").assertIsDisplayed()
        compose.onNodeWithTag("terms_detail_confirm").performClick()
        compose.onNodeWithTag("terms_detail_sheet").assertDoesNotExist()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("account_info_screen").assertIsDisplayed()
    }

    @Test
    fun 오픈라이선스에서_라이브러리를_누르면_팝업이_열리고_닫기를_누르면_닫힌다() {
        openAccountInfo()

        compose.onNodeWithTag("account_info_open_license").performClick()
        compose.onNodeWithTag("open_license_screen").assertIsDisplayed()

        compose.onNodeWithTag("open_license_item_0").performClick()
        compose.onNodeWithTag("open_license_dialog").assertIsDisplayed()
        compose.onNodeWithTag("open_license_dialog_close").performClick()
        compose.onNodeWithTag("open_license_dialog").assertDoesNotExist()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("account_info_screen").assertIsDisplayed()
    }

    @Test
    fun 로그아웃_팝업에서_취소하면_남고_확인하면_로그인으로_이동을_요청한다() {
        openAccountInfo()

        compose.onNodeWithTag("account_info_logout").performClick()
        compose.onNodeWithTag("logout_dialog_cancel").performClick()
        compose.onNodeWithTag("logout_dialog").assertDoesNotExist()
        assertFalse(loginRequested)

        compose.onNodeWithTag("account_info_logout").performClick()
        compose.onNodeWithTag("logout_dialog_confirm").performClick()
        compose.waitForIdle()
        assertTrue(loginRequested)
    }

    @Test
    fun 회원탈퇴_화면에서_탈퇴할게요를_누르면_로그인으로_이동을_요청한다() {
        openAccountInfo()

        compose.onNodeWithTag("account_info_withdraw").performClick()
        compose.onNodeWithTag("withdraw_screen").assertIsDisplayed()
        assertFalse(loginRequested)

        compose.onNodeWithText("탈퇴할게요").performClick()
        compose.waitForIdle()
        assertTrue(loginRequested)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun 공용초대링크에서_리셋하면_로딩_화면_뒤에_새_코드를_보여준다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()
        compose.onNodeWithTag("mypage_menu_invite_link").performScrollTo().performClick()

        // 발급한 초대가 없거나 있으면 그대로 보여준다. 어느 쪽이든 링크 화면이 나온다
        compose.waitUntilExactlyOneExists(hasTestTag("invite_link_screen"), timeoutMillis = 5_000)
        val before = codeText()

        compose.onNodeWithTag("invite_link_reset").performClick()
        compose.onNodeWithTag("invite_link_loading_screen").assertIsDisplayed()

        compose.waitUntilExactlyOneExists(hasTestTag("invite_link_screen"), timeoutMillis = 5_000)
        assertNotEquals(before, codeText())
    }

    // 링크코드 카드 안의 코드 글자
    private fun codeText(): String = compose.onNodeWithTag("invite_link_code", useUnmergedTree = false)
        .fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }

    @Test
    fun 마이페이지_프로필_수정_아이콘을_누르면_닉네임_수정_화면으로_이동하고_뒤로가면_돌아온다() {
        start()
        compose.onNodeWithTag("bottom_nav_mypage").performClick()

        compose.onNodeWithTag("mypage_profile_edit").performClick()
        compose.onNodeWithTag("nickname_edit_screen").assertIsDisplayed()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }
}
