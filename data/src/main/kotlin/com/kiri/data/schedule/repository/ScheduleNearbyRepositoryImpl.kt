package com.kiri.data.schedule.repository

import com.kiri.data.common.network.KiriApiException
import com.kiri.data.common.network.apiResult
import com.kiri.domain.mission.model.NearbyPlace
import com.kiri.domain.family.model.NoFamilyException
import com.kiri.domain.schedule.model.ScheduleNotFoundException
import com.kiri.domain.mission.repository.MissionNearbyRepository
import com.kiri.domain.schedule.repository.ScheduleNearbyRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.kiri.data.schedule.remote.ScheduleNearbyApi
import com.kiri.data.schedule.remote.toNearbyPlace

internal class ScheduleNearbyRepositoryImpl @Inject constructor(
    private val api: ScheduleNearbyApi,
    private val json: Json,
) : ScheduleNearbyRepository, MissionNearbyRepository {

    override suspend fun getMissionNearby(missionId: Long): Result<List<NearbyPlace>> =
        apiResult(json) { api.getMissionNearby(missionId) }
            .map { places -> places.mapIndexed { index, place -> place.toNearbyPlace(index) } }

    // 서버가 정렬한 순서를 그대로 둔다
    override suspend fun getNearby(scheduleId: Long): Result<List<NearbyPlace>> =
        apiResult(json) { api.getNearby(scheduleId) }
            .map { places -> places.mapIndexed { index, place -> place.toNearbyPlace(index) } }
            .recoverCatching { error ->
                throw when ((error as? KiriApiException)?.code) {
                    NO_FAMILY -> NoFamilyException()
                    NOT_FOUND -> ScheduleNotFoundException()
                    else -> error
                }
            }

    private companion object {
        const val NO_FAMILY = "SCHEDULE403_1"
        const val NOT_FOUND = "SCHEDULE404_2"
    }
}
