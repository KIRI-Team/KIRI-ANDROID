package com.kiri.domain.model.condition

import com.kiri.domain.detail.Condition
import com.kiri.domain.model.mission.WalkingLevel

// 약속 당일 컨디션 커스텀 화면에 필요한 일정의 미션 정보와, 지금 그 일정에 적용된 조건
data class ConditionCustom(
    val missionTitle: String,
    // 판단할 정보가 없으면 null
    val walkingLevel: WalkingLevel?,
    val durationMinMinutes: Int?,
    val durationMaxMinutes: Int?,
    // 적용한 조건이 없으면 비어 있다
    val conditions: Set<Condition>,
)

// 수락한 최종 참여자가 아니다 (응답 대기·거절·재확인 필요·초대 안 됨)
class NotFinalParticipantException : Exception("약속의 최종 참여자가 아닙니다.")

// 확정된 일정의 당일이 아니라 컨디션을 보거나 바꿀 수 없다
class ConditionCustomUnavailableException : Exception("일정 당일에만 컨디션을 조회하거나 변경할 수 있습니다.")
