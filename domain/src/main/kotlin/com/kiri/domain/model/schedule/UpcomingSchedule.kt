package com.kiri.domain.model.schedule

import java.time.LocalDate
import java.time.LocalTime

// 다가오는 일정 조회로 받은 카드 두 장. 서로 별개이며 없으면 null 이다
// responseRequired: 내가 아직 응답하지 않은(대기·재확인 필요) 일정 중 응답할 수 있는 가장 가까운 1건
// upcoming: 내가 최종 참여자인 확정 일정 중 D-7 ~ 당일의 가장 가까운 1건
// 취소·미확정·완료 일정은 서버가 빼고 보낸다
data class UpcomingSchedules(
    val responseRequired: UpcomingSchedule? = null,
    val upcoming: UpcomingSchedule? = null,
) {
    // 보여줄 순서. 응답이 필요한 일정을 먼저 둔다
    val cards: List<UpcomingSchedule>
        get() = listOfNotNull(responseRequired, upcoming).distinctBy { it.id }
}

data class UpcomingSchedule(
    val id: Long,
    val date: LocalDate,
    // 읽지 못하면 null
    val time: LocalTime?,
    val type: UpcomingScheduleType,
    // 오늘 기준 시작일까지 남은 일수. 0 이면 당일
    val dDay: Int,
    val title: String,
    // 앱이 모르는 값이면 null
    val myStatus: MyScheduleStatus?,
)

// RESPONSE_REQUIRED: 응답 필요, UPCOMING: 다가오는(D-1 ~ D-7), TODAY: 당일
enum class UpcomingScheduleType { RESPONSE_REQUIRED, UPCOMING, TODAY }
