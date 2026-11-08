package com.kiri.domain.repository

import com.kiri.domain.detail.Condition
import com.kiri.domain.model.condition.ConditionCustom

interface ScheduleConditionRepository {
    // 약속 당일의 컨디션 커스텀 화면 정보를 불러온다
    // 가족 공간이 없으면 NoFamilyException, 최종 참여자가 아니면 NotFinalParticipantException,
    // 없거나 다른 가족의 일정이면 ScheduleNotFoundException, 확정된 일정의 당일이 아니면 ConditionCustomUnavailableException 으로 실패한다
    suspend fun getConditionCustom(scheduleId: Long): Result<ConditionCustom>

    // 고른 조건으로 일정의 기존 조건을 덮어쓴다. 빈 집합이면 모든 조건을 해제한다. 저장한 조건은 근처 놀거리 추천 정렬에 반영된다
    // 실패 이유는 getConditionCustom 과 같다 (당일이 아니면 ConditionCustomUnavailableException)
    suspend fun applyConditions(scheduleId: Long, conditions: Set<Condition>): Result<Unit>
}
