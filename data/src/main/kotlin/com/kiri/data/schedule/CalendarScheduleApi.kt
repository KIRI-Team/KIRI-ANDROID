package com.kiri.data.schedule

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.domain.model.schedule.ScheduleUpdate
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

internal interface CalendarScheduleApi {
    // 로그인 토큰이 필요하다. year·month 가 없거나 범위를 벗어나면 COMMON400_1, 가족 공간이 없으면 SCHEDULE403_1 로 거절된다
    // TODO: 명세에 경로가 없어 추정한 경로다. 서버 명세와 대조할 것
    @GET("api/v1/schedules/calendar")
    suspend fun getMonthlySchedules(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): ApiResponse<MonthlySchedulesResponse>

    // 로그인 토큰이 필요하다. 가족 공간이 없으면 SCHEDULE403_1, 없거나 다른 가족의 일정이면 SCHEDULE404_2,
    // 취소된 일정이면 SCHEDULE409_7 로 거절된다
    // TODO: 명세에 경로가 없어 추정한 경로다. 서버 명세와 대조할 것
    @GET("api/v1/schedules/{scheduleId}")
    suspend fun getScheduleDetail(@Path("scheduleId") scheduleId: Long): ApiResponse<ScheduleDetailResponse>

    // 제안자만 대기·확정 일정을 바꿀 수 있다. 제안자가 아니면 SCHEDULE403_4, 없는 일정이면 SCHEDULE404_2,
    // 바꿀 수 없는 상태면 SCHEDULE409_4, 입력값이 잘못되면 COMMON400_1 로 거절된다
    @PATCH("api/v1/schedules/{scheduleId}")
    suspend fun updateSchedule(
        @Path("scheduleId") scheduleId: Long,
        @Body request: UpdateScheduleRequest,
    ): ApiResponse<kotlinx.serialization.json.JsonElement>
}

// scheduleDate 는 "2026-11-16", scheduleTime 은 "15:00" 처럼 보낸다. 한 줄이 비어 있으면 보내지 않는다
@Serializable
internal data class UpdateScheduleRequest(
    val scheduleDate: String,
    val scheduleTime: String,
    val participantMemberIds: List<Long>,
    // 가족에게 한 줄
    val requestMessage: String? = null,
)

internal fun ScheduleUpdate.toRequest() = UpdateScheduleRequest(
    scheduleDate = date.toString(),
    scheduleTime = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
    participantMemberIds = inviteeIds.sorted(),
    requestMessage = message?.takeIf { it.isNotBlank() },
)

@Serializable
internal data class MonthlySchedulesResponse(
    val year: Int,
    val month: Int,
    val schedules: List<CalendarScheduleResponse> = emptyList(),
)

// date 는 "2026-10-03", time 은 "14:50" 처럼 온다
// 서버가 앱이 모르는 값을 새로 보내도 나머지 일정은 보여줄 수 있도록 문자열로 받는다
@Serializable
internal data class CalendarScheduleResponse(
    val scheduleId: Long,
    val date: String,
    val time: String? = null,
    val status: String,
    val myStatus: String? = null,
)

// 날짜나 일정 상태를 읽지 못하면 캘린더에 표시할 수 없으니 그 일정만 뺀다
internal fun CalendarScheduleResponse.toCalendarSchedule(): CalendarSchedule? {
    val date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val status = CalendarScheduleStatus.entries.firstOrNull { it.name == status } ?: return null
    return CalendarSchedule(
        id = scheduleId,
        date = date,
        time = time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        status = status,
        myStatus = MyScheduleStatus.entries.firstOrNull { it.name == myStatus },
    )
}

// date 는 "2026-11-17", time 은 "14:50" 처럼 온다
@Serializable
internal data class ScheduleDetailResponse(
    val date: String,
    val time: String? = null,
    val status: String? = null,
    val participants: List<ScheduleParticipantResponse> = emptyList(),
)

@Serializable
internal data class ScheduleParticipantResponse(
    val nickname: String,
    val accepted: Boolean,
)

// 날짜를 읽지 못하면 상세를 보여줄 수 없으니 실패로 본다. 시간·상태는 읽지 못해도 나머지는 보여준다
internal fun ScheduleDetailResponse.toScheduleDetail() = ScheduleDetail(
    date = LocalDate.parse(date),
    time = time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    status = ScheduleDisplayStatus.entries.firstOrNull { it.name == status },
    participants = participants.map { ScheduleParticipant(it.nickname, it.accepted) },
)
