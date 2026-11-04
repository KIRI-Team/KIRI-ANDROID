package com.kiri.domain.repository

import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.ScheduleDetail
import java.time.YearMonth

interface CalendarScheduleRepository {
    // 그 달의 가족 일정을 불러온다. 가족 공간이 없으면 빈 목록을 돌려준다
    suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>>

    // 월간 조회로 받은 id 로 일정 상세를 불러온다
    // 가족 공간이 없으면 NoFamilyException, 없거나 다른 가족의 일정이면 ScheduleNotFoundException,
    // 취소된 일정이면 CancelledScheduleException 으로 실패한다
    suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail>
}
