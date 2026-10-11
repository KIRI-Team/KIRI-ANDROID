package com.kiri.app.navigation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.app.HiltTestActivity
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO: 로그인 → 온보딩 완료 → 메인 이동이 연결되면 KiriApp 전체 흐름 테스트 복구
//  (로그인/온보딩을 거쳐 메인에 도달, 메인에서 뒤로가기 시 로그인/온보딩으로 돌아가지 않음)
// HomeScreen이 hiltViewModel()을 쓰므로 Hilt가 적용된 테스트용 Activity에서 MainScreen을 띄운다
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KiriAppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private var loginRequested = false

    private fun setMainContent() {
        composeTestRule.setContent { KiriTheme { MainScreen(onNavigateToLogin = { loginRequested = true }) } }
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

    // 홈 화면은 새 디자인으로 바뀌어 "상세보기" 버튼이 없으므로, 아직 상세 버튼이 있는 기록·장소 탭으로 확인한다
    @Test
    fun 탭을_전환해도_각_탭의_디테일_화면이_독립적으로_유지된다() {
        setMainContent()

        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()
        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("기록 상세").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_place").performClick()
        composeTestRule.onNodeWithTag("place_screen").assertIsDisplayed()

        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("장소 상세").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()
        composeTestRule.onNodeWithText("기록 상세").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_place").performClick()
        composeTestRule.onNodeWithText("장소 상세").assertIsDisplayed()
    }

    @Test
    fun 상세화면에서_뒤로가기_버튼을_누르면_해당_탭의_시작화면으로_돌아간다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_record").performClick()

        composeTestRule.onNodeWithText("상세보기").performClick()
        composeTestRule.onNodeWithText("기록 상세").assertIsDisplayed()

        composeTestRule.onNodeWithText("뒤로").performClick()
        composeTestRule.onNodeWithTag("record_screen").assertIsDisplayed()
    }

    @Test
    fun 마이페이지_메뉴를_누르면_해당_화면으로_이동하고_뒤로가면_마이페이지로_돌아온다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()
        composeTestRule.onNodeWithTag("mypage_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("mypage_menu_notification_setting").performClick()
        composeTestRule.onNodeWithTag("notification_setting_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        composeTestRule.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    @Test
    fun 우리집_주소_설정을_누르면_주소_화면으로_이동하고_뒤로가면_마이페이지로_돌아온다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()

        composeTestRule.onNodeWithTag("mypage_menu_home_address").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("home_address_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        composeTestRule.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    @Test
    fun 가족_기본_설정에서_가족명_수정_화면으로_이동하고_뒤로가면_돌아온다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()
        composeTestRule.onNodeWithTag("mypage_menu_family_setting").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("family_setting_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("family_name_edit").performClick()
        composeTestRule.onNodeWithTag("family_name_edit_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        composeTestRule.onNodeWithTag("family_setting_screen").assertIsDisplayed()
    }

    @Test
    fun 가족_기본_설정에서_내_카드의_수정_아이콘을_누르면_닉네임_수정_화면으로_이동한다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()
        composeTestRule.onNodeWithTag("mypage_menu_family_setting").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("family_member_nickname_edit").performClick()
        composeTestRule.onNodeWithTag("nickname_edit_screen").assertIsDisplayed()
    }

    @Test
    fun 계정_정보를_누르면_계정_정보_화면으로_이동하고_뒤로가면_마이페이지로_돌아온다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()

        composeTestRule.onNodeWithTag("mypage_menu_account_info").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("account_info_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("앱 버전").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        composeTestRule.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    @Test
    fun 건의함에서_내용을_적고_의견_보내기를_누르면_마이페이지로_돌아온다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()

        composeTestRule.onNodeWithTag("mypage_menu_suggestion").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("suggestion_screen").assertIsDisplayed()

        // 내용이 비어 있으면 눌러도 그대로 남는다
        composeTestRule.onNodeWithTag("kiri_button").performClick()
        composeTestRule.onNodeWithTag("suggestion_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("suggestion_content_input").performTextInput("알림이 늦게 와요")
        composeTestRule.onNodeWithText("9/500").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_button").performClick()
        composeTestRule.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }

    private fun openAccountInfo() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()
        composeTestRule.onNodeWithTag("mypage_menu_account_info").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("account_info_screen").assertIsDisplayed()
    }

    @Test
    fun 이용_약관에서_약관을_누르면_세부_내용_창이_열리고_확인을_누르면_닫힌다() {
        openAccountInfo()

        composeTestRule.onNodeWithTag("account_info_terms").performClick()
        composeTestRule.onNodeWithTag("terms_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("terms_item_term_1").performClick()
        composeTestRule.onNodeWithTag("terms_detail_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("terms_detail_confirm").performClick()
        composeTestRule.onNodeWithTag("terms_detail_sheet").assertDoesNotExist()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        composeTestRule.onNodeWithTag("account_info_screen").assertIsDisplayed()
    }

    @Test
    fun 오픈라이선스에서_라이브러리를_누르면_팝업이_열리고_닫기를_누르면_닫힌다() {
        openAccountInfo()

        composeTestRule.onNodeWithTag("account_info_open_license").performClick()
        composeTestRule.onNodeWithTag("open_license_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("open_license_item_0").performClick()
        composeTestRule.onNodeWithTag("open_license_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_license_dialog_close").performClick()
        composeTestRule.onNodeWithTag("open_license_dialog").assertDoesNotExist()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        composeTestRule.onNodeWithTag("account_info_screen").assertIsDisplayed()
    }

    @Test
    fun 로그아웃_팝업에서_취소하면_남고_확인하면_로그인으로_이동을_요청한다() {
        openAccountInfo()

        composeTestRule.onNodeWithTag("account_info_logout").performClick()
        composeTestRule.onNodeWithTag("logout_dialog_cancel").performClick()
        composeTestRule.onNodeWithTag("logout_dialog").assertDoesNotExist()
        assertFalse(loginRequested)

        composeTestRule.onNodeWithTag("account_info_logout").performClick()
        composeTestRule.onNodeWithTag("logout_dialog_confirm").performClick()
        composeTestRule.waitForIdle()
        assertTrue(loginRequested)
    }

    @Test
    fun 회원탈퇴_화면에서_탈퇴할게요를_누르면_로그인으로_이동을_요청한다() {
        openAccountInfo()

        composeTestRule.onNodeWithTag("account_info_withdraw").performClick()
        composeTestRule.onNodeWithTag("withdraw_screen").assertIsDisplayed()
        assertFalse(loginRequested)

        composeTestRule.onNodeWithText("탈퇴할게요").performClick()
        composeTestRule.waitForIdle()
        assertTrue(loginRequested)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun 공용초대링크에서_리셋하면_로딩_화면_뒤에_새_코드를_보여준다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()
        composeTestRule.onNodeWithTag("mypage_menu_invite_link").performScrollTo().performClick()

        // 발급한 초대가 없거나 있으면 그대로 보여준다. 어느 쪽이든 링크 화면이 나온다
        composeTestRule.waitUntilExactlyOneExists(hasTestTag("invite_link_screen"), timeoutMillis = 5_000)
        val before = codeText()

        composeTestRule.onNodeWithTag("invite_link_reset").performClick()
        composeTestRule.onNodeWithTag("invite_link_loading_screen").assertIsDisplayed()

        composeTestRule.waitUntilExactlyOneExists(hasTestTag("invite_link_screen"), timeoutMillis = 5_000)
        assertNotEquals(before, codeText())
    }

    // 링크코드 카드 안의 코드 글자
    private fun codeText(): String = composeTestRule.onNodeWithTag("invite_link_code", useUnmergedTree = false)
        .fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }

    @Test
    fun 마이페이지_프로필_수정_아이콘을_누르면_닉네임_수정_화면으로_이동하고_뒤로가면_돌아온다() {
        setMainContent()
        composeTestRule.onNodeWithTag("bottom_nav_mypage").performClick()

        composeTestRule.onNodeWithTag("mypage_profile_edit").performClick()
        composeTestRule.onNodeWithTag("nickname_edit_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kiri_back_button").performClick()
        composeTestRule.onNodeWithTag("mypage_screen").assertIsDisplayed()
    }
}
