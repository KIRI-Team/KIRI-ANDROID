package com.kiri.data.home

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.*

internal interface HomeApi {
    @GET("api/v1/questions/today")
    suspend fun today(@Header("Authorization") token: String): ApiResponse<TodayQuestionResponse>
    @GET("api/v1/questions/{questionId}/answers")
    suspend fun answers(@Header("Authorization") token: String, @Path("questionId") id: String): ApiResponse<FamilyAnswersResponse>
    @PUT("api/v1/questions/{questionId}/answers/me")
    suspend fun save(@Header("Authorization") token: String, @Path("questionId") id: String, @Body body: AnswerRequest): ApiResponse<kotlinx.serialization.json.JsonElement>
    @POST("api/v1/questions/today/members/{memberId}/pokes")
    suspend fun poke(@Header("Authorization") token: String, @Path("memberId") id: String): ApiResponse<kotlinx.serialization.json.JsonElement>
}

@Serializable internal data class TodayQuestionResponse(val hasFamily: Boolean, val questionId: Long? = null, val question: String? = null, val isAnswered: Boolean = false, val answer: String? = null)
@Serializable internal data class FamilyAnswersResponse(val question: String, val answers: List<MemberAnswer>)
@Serializable internal data class MemberAnswer(val isMe: Boolean, val memberId: Long, val nickname: String, val isAnswered: Boolean, val answer: String? = null)
@Serializable internal data class AnswerRequest(val answer: String)
