package com.kiri.data.bucket

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.bucket.BucketItem
import com.kiri.domain.model.bucket.BucketItemPage
import com.kiri.domain.model.bucket.BucketItemStatus
import com.kiri.domain.model.bucket.BucketSaver
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface BucketItemApi {
    // 로그인 토큰이 필요하다. 가족 공간이 없으면 BUCKET403_1, size 가 1~50 밖이면 COMMON400_1 로 거절된다
    @GET("api/v1/bucket-items")
    suspend fun getBucketItems(
        @Query("memberId") memberId: Long?,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int?,
    ): ApiResponse<BucketItemsResponse>

    // 성공하면 result 가 null 이다. 본인 항목이 아니면 BUCKET403_2, 없거나 다른 가족 항목이면 BUCKET404_1,
    // 일정이 잡혔으면 BUCKET409_3, 완료됐으면 BUCKET409_4 로 거절된다
    @DELETE("api/v1/bucket-items/{bucketItemId}")
    suspend fun deleteBucketItem(@Path("bucketItemId") bucketItemId: Long): ApiResponse<kotlinx.serialization.json.JsonElement>
}

@Serializable
internal data class BucketItemsResponse(
    val savers: List<BucketSaverResponse> = emptyList(),
    val bucketItems: List<BucketItemResponse> = emptyList(),
    val nextCursor: Long? = null,
    val hasNext: Boolean = false,
)

@Serializable
internal data class BucketSaverResponse(
    val memberId: Long,
    val nickname: String,
    val familyRole: String? = null,
    val bucketCount: Int = 0,
)

// savedAt 은 "2026-10-27T19:40:12.345" 처럼 온다. 서버가 앱이 모르는 상태를 보내도 나머지는 보여주도록 문자열로 받는다
@Serializable
internal data class BucketItemResponse(
    val bucketItemId: Long,
    val status: String? = null,
    val savedAt: String? = null,
    val completedAt: String? = null,
    val mission: BucketMissionResponse,
    val saver: BucketItemSaverResponse,
    val schedulable: Boolean = false,
    val deletable: Boolean = false,
    val scheduleId: Long? = null,
    val scheduledDate: String? = null,
)

@Serializable
internal data class BucketMissionResponse(
    val missionId: Long,
    val title: String,
    val imageUrl: String? = null,
    val imageSource: String? = null,
    val status: String? = null,
)

@Serializable
internal data class BucketItemSaverResponse(
    val memberId: Long,
    val nickname: String,
    val familyRole: String? = null,
    val withdrawn: Boolean = false,
)

internal fun BucketItemsResponse.toPage() = BucketItemPage(
    savers = savers.map { BucketSaver(it.memberId, it.nickname, it.bucketCount) },
    items = bucketItems.map { it.toBucketItem() },
    nextCursor = nextCursor,
    hasNext = hasNext,
)

internal fun BucketItemResponse.toBucketItem() = BucketItem(
    id = bucketItemId,
    status = BucketItemStatus.entries.firstOrNull { it.name == status },
    savedAt = savedAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
    missionId = mission.missionId,
    title = mission.title,
    imageUrl = mission.imageUrl,
    missionStatus = mission.status,
    saverId = saver.memberId,
    saverNickname = saver.nickname,
    saverWithdrawn = saver.withdrawn,
    schedulable = schedulable,
    deletable = deletable,
    scheduleId = scheduleId,
    scheduledDate = scheduledDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
)
