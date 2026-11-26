package com.kiri.feature.record.presentation.sheet

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

internal data class RecordCalendarDay(val date: LocalDate, val isInMonth: Boolean)

// 월요일부터 시작하는 주 단위 칸. 첫 주의 앞 칸은 비우고(null), 마지막 주의 남는 칸은 다음 달 날짜로 채운다 (Figma 8450:20828)
internal fun recordCalendarWeeks(month: YearMonth): List<List<RecordCalendarDay?>> {
    val first = month.atDay(1)
    val cells = buildList {
        repeat(first.dayOfWeek.value - DayOfWeek.MONDAY.value) { add(null) }
        var day = first
        while (day.month == first.month) {
            add(RecordCalendarDay(day, isInMonth = true))
            day = day.plusDays(1)
        }
        while (size % DAYS_IN_WEEK != 0) {
            add(RecordCalendarDay(day, isInMonth = false))
            day = day.plusDays(1)
        }
    }
    return cells.chunked(DAYS_IN_WEEK)
}

// earliest 보다 이른 날짜·시간은 고를 수 없다. earliest 가 없으면 모두 고를 수 있다
internal fun isSelectableDate(date: LocalDate, earliest: LocalDateTime?): Boolean =
    earliest == null || !date.isBefore(earliest.toLocalDate())

internal fun isSelectableTime(date: LocalDate?, time: LocalTime, earliest: LocalDateTime?): Boolean =
    earliest == null || date == null || !LocalDateTime.of(date, time).isBefore(earliest)

internal fun YearMonth.toRecordLabel(): String = "${year}년 ${monthValue}월"

private const val DAYS_IN_WEEK = 7
