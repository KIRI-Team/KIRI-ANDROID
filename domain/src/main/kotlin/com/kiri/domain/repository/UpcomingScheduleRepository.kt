package com.kiri.domain.repository

import com.kiri.domain.model.schedule.UpcomingSchedules

// 현재 로그인 회원·가족 기준의 다가오는 일정 카드. 홈 카드와 기록 캘린더에서 함께 쓴다
interface UpcomingScheduleRepository {
    // 가족 공간이 없으면 카드가 없는 것으로 본다
    suspend fun getUpcomingSchedules(): Result<UpcomingSchedules>
}
