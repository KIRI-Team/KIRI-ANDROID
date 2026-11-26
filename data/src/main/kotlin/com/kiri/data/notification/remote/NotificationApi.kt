package com.kiri.data.notification.remote

import com.kiri.data.common.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

internal interface NotificationApi {
    @GET("api/v1/notifications")
    suspend fun list(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<NotificationPageResponse>

    @PATCH("api/v1/notifications/{notificationId}/read")
    suspend fun markRead(@Path("notificationId") notificationId: Long): ApiResponse<Unit>

    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllRead(): ApiResponse<ReadAllResponse>
}

@Serializable
internal data class NotificationPageResponse(
    val content: List<NotificationResponse>,
    val last: Boolean,
)

@Serializable
internal data class NotificationResponse(
    val notificationId: Long,
    val title: String,
    val body: String,
    val notificationType: String,
    val isRead: Boolean,
    val referenceId: Long? = null,
    val createdAt: String,
    val category: String? = null,
    val targetType: String? = null,
)

@Serializable
internal data class ReadAllResponse(val readCount: Int)
