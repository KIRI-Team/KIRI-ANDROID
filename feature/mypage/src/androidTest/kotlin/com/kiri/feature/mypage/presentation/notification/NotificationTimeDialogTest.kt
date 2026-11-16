package com.kiri.feature.mypage.presentation.notification

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.kiri.uikit.KiriTheme
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class NotificationTimeDialogTest {
    @get:Rule val compose = createComposeRule()

    private var saved: LocalTime? = null
    private var dismissed = 0

    private fun show(time: LocalTime = LocalTime.of(9, 0)) {
        compose.setContent {
            KiriTheme {
                NotificationTimeDialog(
                    currentTime = time,
                    currentTimeText = "오전 9시",
                    onSave = { saved = it },
                    onDismiss = { dismissed++ },
                )
            }
        }
    }

    @Test
    fun 처음에는_현재_발송_시각을_보여주고_그대로_저장하면_같은_시각이다() {
        show(LocalTime.of(21, 30))

        compose.onNodeWithTag("notification_time_hour").assertContentDescriptionEquals("21시")
        compose.onNodeWithTag("notification_time_minute").assertContentDescriptionEquals("30분")
        compose.onNodeWithTag("notification_time_save").performClick()

        assertEquals(LocalTime.of(21, 30), saved)
    }

    @Test
    fun 위로_넘기면_뒤의_시각을_고르고_저장한다() {
        show()

        compose.onNodeWithTag("notification_time_hour").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithTag("notification_time_save").performClick()

        val hour = saved!!.hour
        assertNotEquals(9, hour)
        assertEquals(0, saved!!.minute)
    }

    @Test
    fun 천천히_조금만_끌다_놓아도_두_숫자_사이에_멈추지_않고_한_숫자로_붙는다() {
        show()

        // 터치 판정 거리를 넘은 뒤 한 칸(28dp)의 2/3 쯤만 아주 느리게 끌어, fling 속도가 거의 없이 칸 사이에서 놓는다
        compose.onNodeWithTag("notification_time_minute").performTouchInput {
            val drag = viewConfiguration.touchSlop + 20.dp.toPx()
            swipe(start = center, end = center - Offset(0f, drag), durationMillis = 1_500)
        }
        compose.waitForIdle()

        compose.onNodeWithTag("notification_time_minute").assertContentDescriptionEquals("1분")
        compose.onNodeWithTag("notification_time_save").performClick()
        assertEquals(LocalTime.of(9, 1), saved)
    }

    @Test
    fun 처음에서_아래로_넘기면_마지막_값으로_이어진다() {
        show(LocalTime.of(0, 0))

        compose.onNodeWithTag("notification_time_hour").performTouchInput {
            val drag = viewConfiguration.touchSlop + 20.dp.toPx()
            swipe(start = center, end = center + Offset(0f, drag), durationMillis = 1_500)
        }
        compose.waitForIdle()

        compose.onNodeWithTag("notification_time_hour").assertContentDescriptionEquals("23시")
    }

    @Test
    fun 취소와_닫기는_저장하지_않고_닫는다() {
        show()

        compose.onNodeWithTag("notification_time_cancel").performClick()
        compose.onNodeWithTag("notification_time_close").performClick()

        assertNull(saved)
        assertEquals(2, dismissed)
    }
}
