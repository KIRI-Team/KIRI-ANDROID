package com.kiri.data.nearby

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiResult
import com.kiri.domain.detail.NearbyPlace
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.repository.ScheduleNearbyRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class ScheduleNearbyRepositoryImpl @Inject constructor(
    private val api: ScheduleNearbyApi,
    private val json: Json,
) : ScheduleNearbyRepository {

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
