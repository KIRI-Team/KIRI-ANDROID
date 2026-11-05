package com.kiri.domain.repository

import com.kiri.domain.model.mission.MissionInfo

// 서버 미션 API. 일정 상세처럼 미션 id 만 아는 화면이 미션 정보를 채울 때 쓴다
interface MissionInfoRepository {
    // 없거나 노출할 수 없는 미션이면 MissionNotFoundException 으로 실패한다
    suspend fun getMission(missionId: Long): Result<MissionInfo>
}
