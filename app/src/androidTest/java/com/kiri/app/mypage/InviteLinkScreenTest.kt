package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.domain.model.invite.Invite
import com.kiri.feature.mypage.model.invite.InviteLinkUiEvent
import com.kiri.feature.mypage.model.invite.InviteLinkUiState
import com.kiri.feature.mypage.presentation.invite.InviteLinkScreen
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InviteLinkScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<InviteLinkUiEvent>()

    private val invite = Invite(
        code = "NWTKYJ",
        url = "https://kiri.example.com/invite/NWTKYJ",
        expiresAt = LocalDate.of(2026, 10, 12),
    )

    private fun setContent(state: InviteLinkUiState) {
        composeTestRule.setContent {
            KiriTheme { InviteLinkScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 코드와_가린_링크와_만료일이_보인다() {
        setContent(InviteLinkUiState(invite = invite))

        composeTestRule.onNodeWithText("가족이 모이면 더 즐거워져요!").assertIsDisplayed()
        composeTestRule.onNodeWithText("NWTKYJ").assertIsDisplayed()
        composeTestRule.onNodeWithText("kiri.example.com/invite/••••••").assertIsDisplayed()
        composeTestRule.onNodeWithText("10월 12일까지 사용가능").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_button").assertIsEnabled()
    }

    @Test
    fun 복사_리셋_공유_뒤로가기를_이벤트로_전달한다() {
        setContent(InviteLinkUiState(invite = invite))

        composeTestRule.onNodeWithTag("invite_link_code").performClick()
        composeTestRule.onNodeWithTag("invite_link_url").performClick()
        composeTestRule.onNodeWithTag("invite_link_reset").performClick()
        composeTestRule.onNodeWithTag("kiri_button").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(
            listOf(
                InviteLinkUiEvent.CodeCopyClicked,
                InviteLinkUiEvent.LinkCopyClicked,
                InviteLinkUiEvent.ResetClicked,
                InviteLinkUiEvent.ShareClicked,
                InviteLinkUiEvent.BackClicked,
            ),
            events,
        )
    }

    @Test
    fun 발급_중에는_온보딩과_같은_로딩_화면을_보여준다() {
        setContent(InviteLinkUiState(invite = invite, isIssuing = true))

        composeTestRule.onNodeWithTag("invite_link_loading_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("가족코드를\n생성하고 있어요!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_link_screen").assertDoesNotExist()
    }

    @Test
    fun 재발급에_실패하면_코드_대신_실패_문구를_보여주고_리셋할_수_있다() {
        setContent(InviteLinkUiState(isReissueFailed = true))

        composeTestRule.onNodeWithTag("invite_link_reissue_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_link_code").assertDoesNotExist()
        composeTestRule.onNodeWithTag("invite_link_url").assertDoesNotExist()
        composeTestRule.onNodeWithTag("kiri_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("invite_link_reset").performClick()

        assertEquals(listOf(InviteLinkUiEvent.ResetClicked), events)
    }

    @Test
    fun 초대가_없으면_초대보내기를_누를_수_없다() {
        setContent(InviteLinkUiState())

        composeTestRule.onNodeWithTag("kiri_button").assertIsNotEnabled()
    }
}
