package com.kiri.feature.record

import com.kiri.domain.model.schedule.occupiesDay
import com.kiri.domain.repository.CalendarScheduleRepository
import java.time.LocalDate
import java.time.YearMonth

// 하루에는 일정을 하나만 정할 수 있다. 달별 가족 일정으로 이미 일정이 있는 날을 모은다
// 일정 변경에서는 바꾸는 일정 자신이 있는 날을 다시 고를 수 있도록 excludeScheduleId 로 뺀다
internal class ScheduleDayLimit(
    private val schedules: CalendarScheduleRepository,
    private val excludeScheduleId: Long? = null,
) {
    private val months = mutableMapOf<YearMonth, Set<LocalDate>>()

    // 지금까지 불러온 달에서 일정이 있는 날
    val occupiedDates: Set<LocalDate>
        get() = months.values.flatten().toSet()

    // 그 달의 일정을 불러온다. 이미 받은 달은 다시 요청하지 않는다. 불러오지 못하면 false
    suspend fun load(month: YearMonth): Boolean {
        if (month in months) return true
        return schedules.getMonthlySchedules(month)
            .map { list -> list.filter { it.occupiesDay && it.id != excludeScheduleId }.map { it.date }.toSet() }
            .onSuccess { months[month] = it }
            .isSuccess
    }

    companion object {
        const val OCCUPIED_MESSAGE = "이미 약속이 있는 날이에요. 다른 날짜를 골라주세요."
    }
}
