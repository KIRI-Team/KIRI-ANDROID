package com.kiri.data.mission

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiResult
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.model.mission.MissionNotFoundException
import com.kiri.domain.repository.MissionInfoRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class MissionInfoRepositoryImpl @Inject constructor(
    private val api: MissionApi,
    private val json: Json,
) : MissionInfoRepository {

    override suspend fun getMission(missionId: Long): Result<MissionInfo> =
        apiResult(json) { api.getMission(missionId) }
            .map { it.toMissionInfo() }
            .recoverCatching { error ->
                throw if ((error as? KiriApiException)?.code == NOT_FOUND) MissionNotFoundException() else error
            }

    private companion object {
        const val NOT_FOUND = "MISSION404_1"
    }
}
