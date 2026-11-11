package com.kiri.domain.model.schedule

import java.time.LocalDate
import java.time.LocalTime

// 캘린더 월간 조회로 받은 가족 일정 한 건. 제목·참여자 같은 내용은 id 로 상세 일정을 따로 조회한다
// 초대받지 않은 일정도 같은 가족이면 내려오고, 취소한 일정도 숨기지 않고 CANCELLED 로 내려온다
data class CalendarSchedule(
    val id: Long,
    val date: LocalDate,
    // 읽지 못하면 null
    val time: LocalTime?,
    val status: CalendarScheduleStatus,
    // 앱이 모르는 값이면 null
    val myStatus: MyScheduleStatus?,
    // 완료(COMPLETED)된 일정의 리뷰 대표 이미지 주소(서명 URL). 캘린더 날짜 배경으로 쓴다. 없으면 null
    val reviewImageUrl: String? = null,
)

// 월간 조회의 일정 상태 (명세 기준 5가지). 재확인 필요는 일정 상태가 아니라 회원별 응답 상태(myStatus)다
enum class CalendarScheduleStatus { PENDING_RESPONSE, CONFIRMED, NOT_CONFIRMED, CANCELLED, COMPLETED }

// 현재 회원의 응답 상태
enum class MyScheduleStatus { NOT_INVITED, PENDING, ACCEPTED, DECLINED, RECONFIRM_REQUIRED }

// 하루에는 일정을 하나만 정할 수 있다. 응답 대기·확정·완료 일정이 있는 날은 새 일정을 잡을 수 없다
// 취소되었거나 미확정으로 끝난 일정은 그날을 차지하지 않는다
val CalendarSchedule.occupiesDay: Boolean
    get() = status == CalendarScheduleStatus.PENDING_RESPONSE ||
        status == CalendarScheduleStatus.CONFIRMED ||
        status == CalendarScheduleStatus.COMPLETED
