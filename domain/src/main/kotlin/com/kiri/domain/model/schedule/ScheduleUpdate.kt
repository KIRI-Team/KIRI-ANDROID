package com.kiri.domain.model.schedule

import java.time.LocalDate
import java.time.LocalTime

// 제안자가 기존 일정을 바꿀 때 보내는 값. 장소는 버킷(미션)에 묶인 값이라 바꿀 수 없다
data class ScheduleUpdate(
    val date: LocalDate,
    val time: LocalTime,
    // 초대할 가족 구성원의 회원 id. 제안자는 빼고 1명 이상이다
    val inviteeIds: Set<Long>,
    // 가족에게 한 줄. 비어 있으면 보내지 않는다
    val message: String? = null,
)

// 제안자가 아닌 회원은 일정을 바꿀 수 없다
class NotScheduleProposerException : Exception("일정 제안자만 수정할 수 있습니다.")

// 취소·미확정·완료된 일정이나 활동이 시작된 일정은 바꿀 수 없다
class ScheduleNotEditableException : Exception("수정할 수 없는 일정입니다.")

// 날짜·시간 누락, 지난 시각, 운영 기간 밖, 초대 대상 오류처럼 입력값이 잘못됐다
class InvalidScheduleUpdateException(message: String) : Exception(message)

// 상세의 참여자는 제안자가 맨 앞이다. 닉네임은 회원 사이에 겹치지 않으므로 닉네임으로 제안자를 확인한다
fun ScheduleDetail.isProposedBy(nickname: String): Boolean =
    nickname.isNotBlank() && participants.firstOrNull()?.nickname == nickname

// 제안자가 고칠 수 있는 상태. 당일(TODAY)은 상세 화면에서 컨디션 조정을 하고, 완료된 일정은 서버가 수정을 막는다
val ScheduleDetail.canBeEditedBeforeTheDay: Boolean
    get() = status == ScheduleDisplayStatus.SCHEDULED || status == ScheduleDisplayStatus.CONFIRMED
