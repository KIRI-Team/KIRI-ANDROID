package com.kiri.data.schedule

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiResult
import com.kiri.domain.model.schedule.UpcomingSchedules
import com.kiri.domain.repository.UpcomingScheduleRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class UpcomingScheduleRepositoryImpl @Inject constructor(
    private val api: CalendarScheduleApi,
    private val json: Json,
) : UpcomingScheduleRepository {

    // 가족 공간이 없으면 보여줄 일정이 없는 것이므로 오류 대신 빈 카드로 본다
    override suspend fun getUpcomingSchedules(): Result<UpcomingSchedules> =
        apiResult(json) { api.getUpcomingSchedules() }
            .map { it.toUpcomingSchedules() }
            .recoverCatching { error ->
                if (error is KiriApiException && error.code == NO_FAMILY) UpcomingSchedules() else throw error
            }

    private companion object {
        const val NO_FAMILY = "SCHEDULE403_1"
    }
}
