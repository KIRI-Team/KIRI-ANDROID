package com.kiri.feature.record.presentation.calendar

import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

// 다가오는 약속 카드의 일시 줄은 Figma(KIRI/Card/Calendar)처럼 날짜만 보여준다
class RecordScheduleCardTest {
    @Test fun whenLabelShowsDateOnly() {
        val card = UpcomingSchedule(1, LocalDate.of(2026, 6, 23), LocalTime.of(14, 50), UpcomingScheduleType.UPCOMING, 2, "약속", MyScheduleStatus.ACCEPTED)
        assertEquals("일시 I 26.06.23", card.whenLabel())
    }
}
