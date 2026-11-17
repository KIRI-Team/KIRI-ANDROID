package com.kiri.feature.record.presentation.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// 다가오는 약속 카드의 점 세 개는 디자인 요소라 메뉴를 열지 않는다
class ScheduleCardTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun 점_세_개를_눌러도_메뉴_없이_약속_상세를_연다() {
        var opened = 0
        val schedule = UpcomingSchedule(7, LocalDate.of(2026, 11, 20), LocalTime.of(10, 0), UpcomingScheduleType.UPCOMING, 3, "남산 둘레길 걷기", MyScheduleStatus.ACCEPTED)
        compose.setContent { KiriTheme { ScheduleCard(schedule) { opened++ } } }

        compose.onNodeWithTag("record_schedule_more_7").assertIsDisplayed().performClick()

        compose.onNodeWithText("약속 보기").assertDoesNotExist()
        assertEquals(1, opened)
    }
}
