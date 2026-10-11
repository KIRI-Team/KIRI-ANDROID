package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.model.notification.NotificationSettingUiEvent
import com.kiri.feature.mypage.model.notification.NotificationSettingUiState
import com.kiri.feature.mypage.presentation.notification.NotificationSettingScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSettingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<NotificationSettingUiEvent>()

    private fun setContent(state: NotificationSettingUiState) {
        composeTestRule.setContent {
            KiriTheme { NotificationSettingScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 설정과_발송_시각_안내가_보이고_토글을_누르면_이벤트를_전달한다() {
        setContent(NotificationSettingUiState())

        composeTestRule.onNodeWithText("현재 알림 발송 시각 : 오전 9시").assertIsDisplayed()
        composeTestRule.onNodeWithText("푸시 알림을 꺼도 앱 안의 알림함에서 소식을 확인할 수 있어요.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notification_all_toggle").assertIsOn().performClick()
        composeTestRule.onNodeWithTag("notification_regular_toggle").assertIsOn().performClick()
        composeTestRule.onNodeWithTag("notification_regular_time").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(
            listOf(
                NotificationSettingUiEvent.AllToggled,
                NotificationSettingUiEvent.RegularToggled,
                NotificationSettingUiEvent.RegularTimeClicked,
                NotificationSettingUiEvent.BackClicked,
            ),
            events,
        )
    }

    @Test
    fun 전체_알림이_꺼지면_정기_알림과_발송_시각을_누를_수_없다() {
        setContent(NotificationSettingUiState(allEnabled = false))

        composeTestRule.onNodeWithTag("notification_all_toggle").assertIsOff().assertIsEnabled()
        composeTestRule.onNodeWithTag("notification_regular_toggle").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("notification_regular_time").assertIsNotEnabled()
    }

    @Test
    fun 정기_알림만_꺼지면_발송_시각만_누를_수_없다() {
        setContent(NotificationSettingUiState(regularEnabled = false))

        composeTestRule.onNodeWithTag("notification_regular_toggle").assertIsOff().assertIsEnabled()
        composeTestRule.onNodeWithTag("notification_regular_time").assertIsNotEnabled()
    }
}
