package com.kiri.data.schedule.repository

import com.kiri.data.mission.remote.MissionGuideResponse
import com.kiri.data.mission.remote.toMissionGuide
import com.kiri.data.common.network.KiriApiException
import com.kiri.data.common.network.apiCommand
import com.kiri.data.common.network.apiResult
import com.kiri.domain.family.model.NoFamilyException
import com.kiri.domain.schedule.model.CalendarSchedule
import com.kiri.domain.bucket.model.BucketItemNotFoundException
import com.kiri.domain.schedule.model.BucketNotSchedulableException
import com.kiri.domain.schedule.model.CancelledScheduleException
import com.kiri.domain.schedule.model.InvalidScheduleUpdateException
import com.kiri.domain.schedule.model.NotScheduleProposerException
import com.kiri.domain.schedule.model.ScheduleNotEditableException
import com.kiri.domain.schedule.model.ScheduleUpdate
import com.kiri.domain.schedule.model.ScheduleDetail
import com.kiri.domain.schedule.model.ScheduleNotFoundException
import com.kiri.domain.schedule.repository.CalendarScheduleRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.kiri.data.schedule.remote.CalendarScheduleApi
import com.kiri.data.schedule.remote.ScheduleResponseRequest
import com.kiri.data.schedule.remote.toCalendarSchedule
import com.kiri.data.schedule.remote.toRequest
import com.kiri.data.schedule.remote.toScheduleDetail

internal class CalendarScheduleRepositoryImpl @Inject constructor(
    private val api: CalendarScheduleApi,
    private val json: Json,
) : CalendarScheduleRepository {

    override suspend fun createWeeklySchedule(missionId: Long, form: ScheduleUpdate): Result<Long> =
        apiResult(json) { api.createWeekly(missionId, form.toRequest()) }.map { it.scheduleId }

    override suspend fun respond(scheduleId: Long, accept: Boolean): Result<Unit> =
        apiCommand(json) { api.respond(scheduleId, ScheduleResponseRequest(if (accept) "ACCEPT" else "DECLINE")) }
    override suspend fun cancel(scheduleId: Long): Result<Unit> = apiCommand(json) { api.cancel(scheduleId) }

    // 가족 공간이 없으면 보여줄 일정이 없는 것이므로 오류 대신 빈 목록으로 본다
    override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
        apiResult(json) { api.getMonthlySchedules(month.year, month.monthValue) }
            .map { response -> response.schedules.mapNotNull { it.toCalendarSchedule() } }
            .recoverCatching { error ->
                if (error is KiriApiException && error.code == NO_FAMILY) emptyList() else throw error
            }

    override suspend fun getScheduleEssentials(scheduleId: Long) =
        apiResult(json) { api.getScheduleEssentials(scheduleId) }
            .map { items -> items.map(MissionGuideResponse::toMissionGuide) }

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

    override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate): Result<Long> =
        apiResult(json) { api.createSchedule(bucketItemId, form.toRequest()) }
            .map { it.scheduleId }
            .recoverCatching { error ->
                val api = error as? KiriApiException
                throw when (api?.code) {
                    NO_FAMILY -> NoFamilyException()
                    BUCKET_NOT_FOUND -> BucketItemNotFoundException()
                    NOT_SCHEDULABLE -> BucketNotSchedulableException()
                    INVALID_INPUT -> InvalidScheduleUpdateException(api.message)
                    else -> error
                }
            }

    private companion object {
        const val BUCKET_NOT_FOUND = "SCHEDULE404_1"
        const val NOT_SCHEDULABLE = "SCHEDULE409_1"
        const val NO_FAMILY = "SCHEDULE403_1"
        const val NOT_PROPOSER = "SCHEDULE403_4"
        const val NOT_FOUND = "SCHEDULE404_2"
        const val NOT_EDITABLE = "SCHEDULE409_4"
        const val CANCELLED = "SCHEDULE409_7"
        const val INVALID_INPUT = "COMMON400_1"
    }
}
