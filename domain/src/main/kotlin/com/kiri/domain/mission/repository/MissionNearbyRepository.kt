package com.kiri.domain.mission.repository

import com.kiri.domain.mission.model.NearbyPlace

interface MissionNearbyRepository {
    suspend fun getMissionNearby(missionId: Long): Result<List<NearbyPlace>>
}
