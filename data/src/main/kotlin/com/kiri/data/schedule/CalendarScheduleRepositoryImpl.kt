package com.kiri.data.schedule

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.CancelledScheduleException
import com.kiri.domain.model.schedule.InvalidScheduleUpdateException
import com.kiri.domain.model.schedule.NotScheduleProposerException
import com.kiri.domain.model.schedule.ScheduleNotEditableException
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.repository.CalendarScheduleRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class CalendarScheduleRepositoryImpl @Inject constructor(
    private val api: CalendarScheduleApi,
    private val json: Json,
) : CalendarScheduleRepository {

    // 가족 공간이 없으면 보여줄 일정이 없는 것이므로 오류 대신 빈 목록으로 본다
    override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
        apiResult(json) { api.getMonthlySchedules(month.year, month.monthValue) }
            .map { response -> response.schedules.mapNotNull { it.toCalendarSchedule() } }
            .recoverCatching { error ->
                if (error is KiriApiException && error.code == NO_FAMILY) emptyList() else throw error
            }

    override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> =
        apiResult(json) { api.getScheduleDetail(scheduleId) }
            .mapCatching { it.toScheduleDetail() }
            .recoverCatching { error ->
                throw when ((error as? KiriApiException)?.code) {
                    NO_FAMILY -> NoFamilyException()
                    NOT_FOUND -> ScheduleNotFoundException()
                    CANCELLED -> CancelledScheduleException()
                    else -> error
                }
            }

    override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit> =
        apiCommand(json) { api.updateSchedule(scheduleId, update.toRequest()) }
            .recoverCatching { error ->
                val api = error as? KiriApiException
                throw when (api?.code) {
                    NOT_PROPOSER -> NotScheduleProposerException()
                    NOT_FOUND -> ScheduleNotFoundException()
                    NOT_EDITABLE -> ScheduleNotEditableException()
                    INVALID_INPUT -> InvalidScheduleUpdateException(api.message)
                    else -> error
                }
            }

    private companion object {
        const val NO_FAMILY = "SCHEDULE403_1"
        const val NOT_PROPOSER = "SCHEDULE403_4"
        const val NOT_FOUND = "SCHEDULE404_2"
        const val NOT_EDITABLE = "SCHEDULE409_4"
        const val CANCELLED = "SCHEDULE409_7"
        const val INVALID_INPUT = "COMMON400_1"
    }
}
