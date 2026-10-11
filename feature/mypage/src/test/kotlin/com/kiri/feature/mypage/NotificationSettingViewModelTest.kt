package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.notification.NotificationSettingUiEffect
import com.kiri.feature.mypage.model.notification.NotificationSettingUiEvent
import com.kiri.feature.mypage.model.notification.NotificationSettingUiState
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingViewModelTest {

    private val viewModel = NotificationSettingViewModel()
    private val state get() = viewModel.uiState.value

    @Test
    fun 처음에는_모든_알림이_켜져_있고_발송_시각은_오전_9시다() {
        assertTrue(state.allEnabled)
        assertTrue(state.regularEnabled)
        assertEquals("오전 9시", state.regularTimeText)
        assertTrue(state.canChangeRegularTime)
    }

    @Test
    fun 정기_알림을_끄면_발송_시각을_바꿀_수_없다() {
        viewModel.onEvent(NotificationSettingUiEvent.RegularToggled)

        assertFalse(state.regularEnabled)
        assertTrue(state.allEnabled)
        assertFalse(state.canChangeRegularTime)
    }

    @Test
    fun 전체_알림을_끄면_정기_알림을_바꿀_수_없고_정기_알림_값은_그대로_둔다() {
        viewModel.onEvent(NotificationSettingUiEvent.AllToggled)

        assertFalse(state.allEnabled)
        assertFalse(state.canChangeRegular)
        assertFalse(state.canChangeRegularTime)

        viewModel.onEvent(NotificationSettingUiEvent.RegularToggled)
        assertTrue(state.regularEnabled)
    }

    @Test
    fun 전체_알림을_다시_켜면_정기_알림_설정이_되살아난다() {
        viewModel.onEvent(NotificationSettingUiEvent.AllToggled)
        viewModel.onEvent(NotificationSettingUiEvent.AllToggled)

        assertTrue(state.allEnabled)
        assertTrue(state.canChangeRegularTime)
    }

    @Test
    fun 뒤로가기를_누르면_마이페이지로_돌아간다() = runTest {
        viewModel.onEvent(NotificationSettingUiEvent.BackClicked)

        assertEquals(NotificationSettingUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 발송_시각은_오전_오후와_12시를_한국식으로_보여준다() {
        fun text(hour: Int, minute: Int = 0) = NotificationSettingUiState(regularTime = LocalTime.of(hour, minute)).regularTimeText

        assertEquals("오전 12시", text(0))
        assertEquals("오전 11시", text(11))
        assertEquals("오후 12시", text(12))
        assertEquals("오후 3시 30분", text(15, 30))
    }
}
