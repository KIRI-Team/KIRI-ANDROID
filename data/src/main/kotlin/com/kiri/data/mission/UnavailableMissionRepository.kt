package com.kiri.data.mission

import com.kiri.domain.mission.MissionContent
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.place.PlaceMission

/**
 * The server has no mission API yet. Demo catalogues are only constructed explicitly by
 * tests/previews.
 */
class UnavailableMissionRepository : MissionRepository {
    override suspend fun catalog(): List<PlaceMission> = emptyList()

    override suspend fun load(missionId: String): MissionContent =
        error("미션 정보를 아직 제공하지 않아요. 잠시 후 다시 확인해주세요.")
}
