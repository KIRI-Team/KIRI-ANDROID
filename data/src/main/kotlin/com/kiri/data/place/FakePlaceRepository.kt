package com.kiri.data.place

import com.kiri.data.mission.FakeMissionRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.place.PlaceMission
import com.kiri.domain.record.BucketRepository

/** Explicit demo catalogue for tests and previews, never the application binding. */
class FakePlaceRepository(
    userId: String = "demo-user",
    missions: List<PlaceMission>? = null,
    pageSize: Int = 12,
    missionRepository: MissionRepository = FakeMissionRepository(),
    familyBuckets: BucketRepository? = null,
) : LocalPlaceRepository(userId, missions, pageSize, missionRepository, familyBuckets) {
    companion object {
        fun demoMissions(): List<PlaceMission> = FakeMissionRepository.demoCatalog()
    }
}
