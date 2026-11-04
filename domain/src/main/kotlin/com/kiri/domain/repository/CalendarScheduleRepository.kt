package com.kiri.domain.repository

import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleUpdate
import java.time.YearMonth

interface CalendarScheduleRepository {
    // 그 달의 가족 일정을 불러온다. 가족 공간이 없으면 빈 목록을 돌려준다
    suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>>

    // 월간 조회로 받은 id 로 일정 상세를 불러온다
    // 가족 공간이 없으면 NoFamilyException, 없거나 다른 가족의 일정이면 ScheduleNotFoundException,
    // 취소된 일정이면 CancelledScheduleException 으로 실패한다
    suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail>

    // 제안자가 일정의 날짜·시간·참여자·한 줄을 바꾼다. 바뀌면 기존 수락자는 다시 응답해야 한다
    // 제안자가 아니면 NotScheduleProposerException, 없는 일정이면 ScheduleNotFoundException,
    // 바꿀 수 없는 상태면 ScheduleNotEditableException, 입력값이 잘못되면 InvalidScheduleUpdateException 으로 실패한다
    suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit>
}
