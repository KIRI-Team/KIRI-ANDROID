package com.kiri.domain.repository

import com.kiri.domain.detail.NearbyPlace

interface MissionNearbyRepository {
    suspend fun getMissionNearby(missionId: Long): Result<List<NearbyPlace>>
}
