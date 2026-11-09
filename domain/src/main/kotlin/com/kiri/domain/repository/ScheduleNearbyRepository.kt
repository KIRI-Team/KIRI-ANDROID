package com.kiri.domain.repository

import com.kiri.domain.detail.NearbyPlace

interface ScheduleNearbyRepository {
    // 일정 장소에서 반경 5km 안의 함께 들르기 좋은 장소를 서버가 정렬한 순서대로 최대 5개 불러온다
    // 정렬에는 가족 취향과 일정에 저장된 컨디션 조건이 반영된다. 추천할 장소가 없으면 빈 목록이다
    // 가족 공간이 없으면 NoFamilyException, 없거나 다른 가족의 일정이면 ScheduleNotFoundException 으로 실패한다
    suspend fun getNearby(scheduleId: Long): Result<List<NearbyPlace>>
}
