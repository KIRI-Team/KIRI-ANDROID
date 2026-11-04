package com.kiri.domain.model.schedule

import java.time.LocalDate
import java.time.LocalTime

// 일정 상세. 같은 가족의 일정이면 초대받지 않았어도 볼 수 있다
data class ScheduleDetail(
    val date: LocalDate,
    // 읽지 못하면 null
    val time: LocalTime?,
    // 앱이 모르는 값이면 null
    val status: ScheduleDisplayStatus?,
    // 제안자가 맨 앞이고 이후 초대 순서다. 탈퇴한 회원의 닉네임은 "탈퇴한 사용자"로 온다
    val participants: List<ScheduleParticipant>,
)

data class ScheduleParticipant(
    val nickname: String,
    // 수락했으면 true, 응답 대기·거절이면 false
    val accepted: Boolean,
)

// 화면 표시용 일정 상태
enum class ScheduleDisplayStatus(val label: String) {
    SCHEDULED("약속 예정"),
    CONFIRMED("약속 확정"),
    TODAY("약속 당일이에요!"),
    COMPLETED("약속 완료"),
}

// 없거나 다른 가족의 일정이다
class ScheduleNotFoundException : Exception("일정을 찾을 수 없습니다.")

// 취소된 일정은 상세를 볼 수 없다
class CancelledScheduleException : Exception("취소된 일정입니다.")
