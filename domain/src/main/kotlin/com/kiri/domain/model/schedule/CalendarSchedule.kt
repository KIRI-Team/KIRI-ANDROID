package com.kiri.domain.model.schedule

import com.kiri.domain.home.ScheduleStatus
import java.time.LocalDate
import java.time.LocalTime

// 캘린더 월간 조회로 받은 가족 일정 한 건. 제목·참여자 같은 내용은 id 로 상세 일정을 따로 조회한다
// 초대받지 않은 일정도 같은 가족이면 내려오고, 취소한 일정도 숨기지 않고 CANCELLED 로 내려온다
data class CalendarSchedule(
    val id: Long,
    val date: LocalDate,
    // 읽지 못하면 null
    val time: LocalTime?,
    val status: ScheduleStatus,
    // 앱이 모르는 값이면 null
    val myStatus: MyScheduleStatus?,
)

// 현재 회원의 응답 상태
enum class MyScheduleStatus { NOT_INVITED, PENDING, ACCEPTED, DECLINED, RECONFIRM_REQUIRED }
