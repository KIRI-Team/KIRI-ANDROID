package com.kiri.data.condition

import com.kiri.data.network.ApiResponse
import com.kiri.domain.detail.Condition
import com.kiri.domain.model.condition.ConditionCustom
import com.kiri.domain.model.mission.WalkingLevel
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface ScheduleConditionApi {
    // 로그인 토큰이 필요하다. 확정된 일정의 당일에, 수락한 최종 참여자만 조회할 수 있다
    // 가족 공간이 없으면 SCHEDULE403_1, 최종 참여자가 아니면 SCHEDULE403_5, 없거나 다른 가족의 일정이면 SCHEDULE404_2,
    // 확정되지 않았거나 당일이 아니면 SCHEDULE409_6 으로 거절된다
    // 전달받은 경로는 schedule/{scheduleId}/condition 이다. 다른 일정 API 처럼 api/v1/schedules 아래에 둔다
    // TODO: schedule 이 단수인지 서버 명세와 대조할 것
    @GET("api/v1/schedules/{scheduleId}/condition")
    suspend fun getConditionCustom(@Path("scheduleId") scheduleId: Long): ApiResponse<ConditionCustomResponse>

    // 같은 경로에 PUT. 보낸 목록으로 기존 조건을 덮어쓰고, 빈 배열이면 모두 해제한다
    // 최종 참여자가 아니면 SCHEDULE403_5, 확정된 일정의 당일이 아니면 SCHEDULE409_5 로 거절된다 (그 밖은 조회와 같다)
    @PUT("api/v1/schedules/{scheduleId}/condition")
    suspend fun applyConditions(
        @Path("scheduleId") scheduleId: Long,
        @Body request: ApplyConditionsRequest,
    ): ApiResponse<kotlinx.serialization.json.JsonElement>
}

// {"conditions": ["REDUCE_STEPS", "MORE_REST"]}
// conditions 를 빼거나 null 로 보내면 400 이라, 빈 목록도 항상 보내도록 기본값을 두지 않는다
@Serializable
internal data class ApplyConditionsRequest(val conditions: List<String>)

// 걷기량·조건은 앱이 모르는 값이 와도 나머지를 읽을 수 있도록 문자열로 받는다
@Serializable
internal data class ConditionCustomResponse(
    val missionTitle: String = "",
    val walkingLevel: String? = null,
    val durationMinMinutes: Int? = null,
    val durationMaxMinutes: Int? = null,
    val conditions: List<String> = emptyList(),
)

internal fun ConditionCustomResponse.toConditionCustom() = ConditionCustom(
    missionTitle = missionTitle,
    walkingLevel = WalkingLevel.entries.firstOrNull { it.name == walkingLevel },
    durationMinMinutes = durationMinMinutes,
    durationMaxMinutes = durationMaxMinutes,
    conditions = conditions.mapNotNull(::toCondition).toSet(),
)

// 앱의 조건을 서버의 조건 이름으로 바꾼다. 항상 같은 순서로 보낸다
internal fun Set<Condition>.toRequest() = ApplyConditionsRequest(
    Condition.entries.filter { it in this }.map { it.serverName },
)

internal val Condition.serverName: String
    get() = when (this) {
        Condition.LESS_WALKING -> "REDUCE_STEPS"
        Condition.MORE_REST -> "MORE_REST"
        Condition.LOWER_BUDGET -> "LOWER_BUDGET"
        Condition.AVOID_CROWDS -> "AVOID_CROWD"
    }

// 서버의 조건 이름을 앱의 조건으로 바꾼다. 모르는 값은 뺀다
internal fun toCondition(value: String): Condition? = when (value) {
    "REDUCE_STEPS" -> Condition.LESS_WALKING
    "MORE_REST" -> Condition.MORE_REST
    "LOWER_BUDGET" -> Condition.LOWER_BUDGET
    "AVOID_CROWD" -> Condition.AVOID_CROWDS
    else -> null
}
