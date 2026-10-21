package com.kiri.feature.record.presentation.sheet

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordCalendarTest {

    @Test
    fun `가장 이른 시각보다 앞선 날짜와 시간은 고를 수 없다`() {
        val earliest = LocalDateTime.of(2026, 10, 18, 14, 10)
        assertFalse(isSelectableDate(LocalDate.of(2026, 10, 17), earliest))
        assertTrue(isSelectableDate(LocalDate.of(2026, 10, 18), earliest))
        assertTrue(isSelectableDate(LocalDate.of(2026, 10, 19), earliest))

        val today = LocalDate.of(2026, 10, 18)
        assertFalse(isSelectableTime(today, LocalTime.of(14, 0), earliest))
        assertTrue(isSelectableTime(today, LocalTime.of(14, 30), earliest))
        // 다음 날이면 이른 시간도 고를 수 있다
        assertTrue(isSelectableTime(today.plusDays(1), LocalTime.of(9, 0), earliest))
        // 날짜를 아직 안 골랐거나 제한이 없으면 막지 않는다
        assertTrue(isSelectableTime(null, LocalTime.of(9, 0), earliest))
        assertTrue(isSelectableTime(today, LocalTime.of(9, 0), null))
        assertTrue(isSelectableDate(LocalDate.of(2000, 1, 1), null))
    }

    @Test
    fun `월요일 시작 달력에서 첫 주 앞 칸은 비우고 마지막 주는 다음 달 날짜로 채운다`() {
        // 2026년 12월 1일은 화요일
        val weeks = recordCalendarWeeks(YearMonth.of(2026, 12))

        assertEquals(5, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertNull(weeks.first()[0])
        assertEquals(RecordCalendarDay(LocalDate.of(2026, 12, 1), isInMonth = true), weeks.first()[1])
        assertEquals(
            listOf(28, 29, 30, 31, 1, 2, 3),
            weeks.last().map { it!!.date.dayOfMonth },
        )
        assertEquals(listOf(true, true, true, true, false, false, false), weeks.last().map { it!!.isInMonth })
    }

    @Test
    fun `월요일에 시작하는 달은 앞 칸을 비우지 않는다`() {
        // 2026년 6월 1일은 월요일
        val weeks = recordCalendarWeeks(YearMonth.of(2026, 6))

        assertEquals(LocalDate.of(2026, 6, 1), weeks.first()[0]?.date)
    }

    @Test
    fun `일요일에 끝나는 달은 다음 달 날짜를 붙이지 않는다`() {
        // 2026년 5월 1일은 금요일, 31일은 일요일이라 앞 칸 4개가 비고 딱 5주로 끝난다
        val weeks = recordCalendarWeeks(YearMonth.of(2026, 5))

        assertEquals(5, weeks.size)
        assertEquals(4, weeks.first().count { it == null })
        assertEquals(LocalDate.of(2026, 5, 31), weeks.last().last()?.date)
        assertTrue(weeks.flatten().filterNotNull().all { it.isInMonth })
    }

    @Test
    fun `월 이름은 연도와 월로 보여준다`() {
        assertEquals("2026년 12월", YearMonth.of(2026, 12).toRecordLabel())
    }
}
