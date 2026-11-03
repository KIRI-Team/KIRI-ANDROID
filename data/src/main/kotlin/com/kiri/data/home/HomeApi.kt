package com.kiri.data.home

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.*

// 모두 로그인 토큰이 필요하다. Authorization 헤더는 AuthInterceptor 가 붙인다
internal interface HomeApi {
    @GET("api/v1/schedules/upcoming")
    suspend fun upcoming(): ApiResponse<UpcomingSchedules>
    @GET("api/v1/questions/today")
    suspend fun today(): ApiResponse<TodayQuestionResponse>
    @GET("api/v1/questions/{questionId}/answers")
    suspend fun answers(@Path("questionId") id: String): ApiResponse<FamilyAnswersResponse>
    @PUT("api/v1/questions/{questionId}/answers/me")
    suspend fun save(@Path("questionId") id: String, @Body body: AnswerRequest): ApiResponse<kotlinx.serialization.json.JsonElement>
    @POST("api/v1/questions/today/members/{memberId}/pokes")
    suspend fun poke(@Path("memberId") id: String): ApiResponse<kotlinx.serialization.json.JsonElement>
}

@Serializable internal data class TodayQuestionResponse(val hasFamily: Boolean, val questionId: Long? = null, val question: String? = null, val isAnswered: Boolean = false, val answer: String? = null)
@Serializable internal data class FamilyAnswersResponse(val question: String, val answers: List<MemberAnswer>)
@Serializable internal data class MemberAnswer(val isMe: Boolean, val memberId: Long, val nickname: String, val isAnswered: Boolean, val answer: String? = null)
@Serializable internal data class AnswerRequest(val answer: String)

@Serializable internal data class UpcomingSchedules(val responseRequired: UpcomingSchedule? = null, val upcoming: UpcomingSchedule? = null)
@Serializable internal data class UpcomingSchedule(val scheduleId: Long, val date: String, val time: String,
    val status: String, val myStatus: String, val type: String, val title: String)
