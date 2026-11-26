package com.kiri.data.mission.repository

import com.kiri.data.common.network.KiriApiException
import com.kiri.data.common.network.apiResult
import com.kiri.domain.mission.model.MissionGuide
import com.kiri.domain.mission.model.MissionInfo
import com.kiri.domain.mission.model.MissionNotFoundException
import com.kiri.domain.mission.repository.MissionInfoRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.kiri.data.mission.remote.MissionApi
import com.kiri.data.mission.remote.MissionGuideResponse
import com.kiri.data.mission.remote.toMissionGuide
import com.kiri.data.mission.remote.toMissionInfo

internal class MissionInfoRepositoryImpl @Inject constructor(
    private val api: MissionApi,
    private val json: Json,
) : MissionInfoRepository {

    override suspend fun saveBucket(missionId: Long): Result<Unit> =
        com.kiri.data.common.network.apiCommand(json) { api.saveBucket(missionId) }

    override suspend fun saveBucket(missionId: Long, kiriPickId: Long): Result<Unit> =
        com.kiri.data.common.network.apiCommand(json) { api.saveBucket(missionId, kiriPickId) }

    override suspend fun getMission(missionId: Long): Result<MissionInfo> =
        apiResult(json) { api.getMission(missionId) }
            .map { it.toMissionInfo() }
            .recoverCatching { error ->
                throw if ((error as? KiriApiException)?.code == NOT_FOUND) MissionNotFoundException() else error
            }

    override suspend fun getMissionEssentials(missionId: Long): Result<List<MissionGuide>> =
        apiResult(json) { api.getMissionEssentials(missionId) }
            .map { items -> items.map(MissionGuideResponse::toMissionGuide) }

    private companion object {
        const val NOT_FOUND = "MISSION404_1"
    }
}
