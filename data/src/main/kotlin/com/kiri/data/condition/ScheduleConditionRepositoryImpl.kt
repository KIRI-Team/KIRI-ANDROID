package com.kiri.data.condition

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.detail.Condition
import com.kiri.domain.model.condition.ConditionCustom
import com.kiri.domain.model.condition.ConditionCustomUnavailableException
import com.kiri.domain.model.condition.NotFinalParticipantException
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.repository.ScheduleConditionRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class ScheduleConditionRepositoryImpl @Inject constructor(
    private val api: ScheduleConditionApi,
    private val json: Json,
) : ScheduleConditionRepository {

    override suspend fun getConditionCustom(scheduleId: Long): Result<ConditionCustom> =
        apiResult(json) { api.getConditionCustom(scheduleId) }
            .map { it.toConditionCustom() }
            .recoverCatching { throw it.toDomainError() }

    override suspend fun applyConditions(scheduleId: Long, conditions: Set<Condition>): Result<Unit> =
        apiCommand(json) { api.applyConditions(scheduleId, conditions.toRequest()) }
            .recoverCatching { throw it.toDomainError() }

    private fun Throwable.toDomainError(): Throwable = when ((this as? KiriApiException)?.code) {
        NO_FAMILY -> NoFamilyException()
        NOT_FINAL_PARTICIPANT -> NotFinalParticipantException()
        NOT_FOUND -> ScheduleNotFoundException()
        NOT_TODAY_TO_READ, NOT_TODAY_TO_APPLY -> ConditionCustomUnavailableException()
        else -> this
    }

    private companion object {
        const val NO_FAMILY = "SCHEDULE403_1"
        const val NOT_FINAL_PARTICIPANT = "SCHEDULE403_5"
        const val NOT_FOUND = "SCHEDULE404_2"
        // 조회는 409_6, 적용은 409_5 로 같은 이유(확정된 일정의 당일이 아님)를 알려준다
        const val NOT_TODAY_TO_READ = "SCHEDULE409_6"
        const val NOT_TODAY_TO_APPLY = "SCHEDULE409_5"
    }
}
