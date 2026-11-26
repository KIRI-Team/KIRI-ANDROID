package com.kiri.feature.record.presentation.calendar

import com.kiri.feature.record.model.RecordCalendarMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 약속이 있는 날짜 칸은 날짜에 따라 예정·지난·오늘 모양으로 나눈다 (Figma 10693:9281)
class ScheduleCellLookTest {
    @Test fun scheduleCellsFollowTheDateNotTheStatus() {
        // 대기·확정 상관없이 앞으로의 약속은 예정, 지난 날짜는 지난 약속
        assertEquals(ScheduleCellLook.UPCOMING, scheduleCellLook(RecordCalendarMark.PLANNED, today = false, past = false))
        assertEquals(ScheduleCellLook.UPCOMING, scheduleCellLook(RecordCalendarMark.CONFIRMED, today = false, past = false))
        assertEquals(ScheduleCellLook.PAST, scheduleCellLook(RecordCalendarMark.PLANNED, today = false, past = true))
        assertEquals(ScheduleCellLook.PAST, scheduleCellLook(RecordCalendarMark.CONFIRMED, today = false, past = true))
        // 오늘 약속은 초록 시계
        assertEquals(ScheduleCellLook.TODAY, scheduleCellLook(RecordCalendarMark.CONFIRMED, today = true, past = false))
    }

    @Test fun cancelledOrUnconfirmedLooksLikeAPastSchedule() {
        assertEquals(ScheduleCellLook.PAST, scheduleCellLook(RecordCalendarMark.CANCELLED, today = false, past = false))
        assertEquals(ScheduleCellLook.PAST, scheduleCellLook(RecordCalendarMark.CANCELLED, today = true, past = false))
    }

    @Test fun otherMarksKeepTheirOwnLook() {
        assertNull(scheduleCellLook(null, today = true, past = false))
        assertNull(scheduleCellLook(RecordCalendarMark.QUESTION, today = false, past = true))
        assertNull(scheduleCellLook(RecordCalendarMark.BUCKET, today = false, past = false))
        assertNull(scheduleCellLook(RecordCalendarMark.PHOTO_FIRST, today = false, past = true))
    }
}
