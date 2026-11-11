package com.kiri.data.schedule

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import com.kiri.domain.model.schedule.UpcomingSchedules
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface CalendarScheduleApi {
    @PATCH("api/v1/schedules/{scheduleId}/response")
    suspend fun respond(@Path("scheduleId") scheduleId: Long, @Body request: ScheduleResponseRequest): ApiResponse<kotlinx.serialization.json.JsonElement>
    @PATCH("api/v1/schedules/{scheduleId}/cancel")
    suspend fun cancel(@Path("scheduleId") scheduleId: Long): ApiResponse<kotlinx.serialization.json.JsonElement>

    @POST("api/v1/missions/{missionId}/schedules")
    suspend fun createWeekly(@Path("missionId") missionId: Long, @Body request: UpdateScheduleRequest): ApiResponse<CreateScheduleResponse>

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
    // 로그인 토큰이 필요하다. 파라미터 없이 현재 회원·가족 기준으로 응답 필요 1건, 다가오는 1건을 돌려준다
    // 가족 공간이 없으면 SCHEDULE403_1 로 거절된다
    // TODO: 명세에 경로가 없어 추정한 경로다. 서버 명세와 대조할 것
    @GET("api/v1/schedules/upcoming")
    suspend fun getUpcomingSchedules(): ApiResponse<UpcomingSchedulesResponse>

    @GET("api/v1/schedules/{scheduleId}")
    suspend fun getScheduleDetail(@Path("scheduleId") scheduleId: Long): ApiResponse<ScheduleDetailResponse>

    // 제안자만 대기·확정 일정을 바꿀 수 있다. 제안자가 아니면 SCHEDULE403_4, 없는 일정이면 SCHEDULE404_2,
    // 바꿀 수 없는 상태면 SCHEDULE409_4, 입력값이 잘못되면 COMMON400_1 로 거절된다
    @PATCH("api/v1/schedules/{scheduleId}")
    suspend fun updateSchedule(
        @Path("scheduleId") scheduleId: Long,
        @Body request: UpdateScheduleRequest,
    ): ApiResponse<kotlinx.serialization.json.JsonElement>

    // 버킷의 일정 잡기. SAVED 이고 활성 일정이 없는 버킷에서만 만들 수 있다
    // 가족 공간이 없으면 SCHEDULE403_1, 없는 버킷이면 SCHEDULE404_1, 만들 수 없는 버킷이면 SCHEDULE409_1,
    // 입력값이 잘못되면 COMMON400_1 로 거절된다
    // 요청 본문은 일정 수정과 같다 (scheduleDate, scheduleTime, participantMemberIds, requestMessage)
    @POST("api/v1/bucket-items/{bucketItemId}/schedules")
    suspend fun createSchedule(
        @Path("bucketItemId") bucketItemId: Long,
        @Body request: UpdateScheduleRequest,
    ): ApiResponse<CreateScheduleResponse>
}

@Serializable internal data class ScheduleResponseRequest(val action: String)

@Serializable
internal data class CreateScheduleResponse(val scheduleId: Long)

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
    // 완료된 일정에만 온다. 리뷰 대표 이미지(서명 URL)
    val reviewImageUrl: String? = null,
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
        // 완료된 일정의 이미지만 날짜 배경으로 쓴다. 빈 주소는 버린다
        reviewImageUrl = reviewImageUrl?.takeIf { status == CalendarScheduleStatus.COMPLETED && it.isNotBlank() },
    )
}

// date 는 "2026-11-17", time 은 "14:50" 처럼 온다
@Serializable
internal data class ScheduleDetailResponse(
    val missionId: Long? = null,
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
    missionId = missionId,
)

@Serializable
internal data class UpcomingSchedulesResponse(
    val responseRequired: UpcomingScheduleResponse? = null,
    val upcoming: UpcomingScheduleResponse? = null,
)

// date 는 "2026-11-20", time 은 "18:00" 처럼 온다. 상태 값은 앱이 모르는 값이 와도 읽을 수 있도록 문자열로 받는다
@Serializable
internal data class UpcomingScheduleResponse(
    val scheduleId: Long,
    val date: String,
    val time: String? = null,
    val status: String? = null,
    val myStatus: String? = null,
    val dDay: Int? = null,
    val type: String? = null,
    val title: String? = null,
)

internal fun UpcomingSchedulesResponse.toUpcomingSchedules() = UpcomingSchedules(
    responseRequired = responseRequired?.toUpcomingSchedule(UpcomingScheduleType.RESPONSE_REQUIRED),
    upcoming = upcoming?.toUpcomingSchedule(UpcomingScheduleType.UPCOMING),
)

// 날짜를 읽지 못하면 카드를 보여줄 수 없으니 그 카드만 뺀다
// type 을 모르면 받은 칸(응답 필요·다가오는)으로 정하고, dDay 가 없으면 날짜로 셀 수 없으니 0 으로 둔다
internal fun UpcomingScheduleResponse.toUpcomingSchedule(slot: UpcomingScheduleType): UpcomingSchedule? {
    val date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return UpcomingSchedule(
        id = scheduleId,
        date = date,
        time = time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        type = UpcomingScheduleType.entries.firstOrNull { it.name == type } ?: slot,
        dDay = dDay ?: 0,
        title = title?.takeIf { it.isNotBlank() } ?: "${date.monthValue}월 ${date.dayOfMonth}일 약속",
        myStatus = MyScheduleStatus.entries.firstOrNull { it.name == myStatus },
    )
}
