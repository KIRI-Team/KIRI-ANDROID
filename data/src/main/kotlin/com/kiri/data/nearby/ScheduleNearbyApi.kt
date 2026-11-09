package com.kiri.data.nearby

import com.kiri.data.network.ApiResponse
import com.kiri.domain.detail.NearbyPlace
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

internal interface ScheduleNearbyApi {
    // 로그인 토큰이 필요하다. result 는 정렬된 장소 배열(최대 5개)이며 없으면 빈 배열이다
    // 가족 공간이 없으면 SCHEDULE403_1, 없거나 다른 가족의 일정이면 SCHEDULE404_2 로 거절된다
    // 전달받은 경로는 schedule/{scheduleId}/nearby 이다. 컨디션 API 와 같이 api/v1/schedules 아래에 둔다
    // TODO: schedule 이 단수인지 서버 명세와 대조할 것
    @GET("api/v1/schedules/{scheduleId}/nearby")
    suspend fun getNearby(@Path("scheduleId") scheduleId: Long): ApiResponse<List<NearbyPlaceResponse>>
}

// region 은 "서울시 서초구" 처럼 시·도와 시·군·구까지만 온다
@Serializable
internal data class NearbyPlaceResponse(
    val placeName: String,
    val region: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val price: Int? = null,
)

// 서버가 장소 id 를 주지 않아 순서와 이름으로 목록 안에서만 쓰는 id 를 만든다
internal fun NearbyPlaceResponse.toNearbyPlace(index: Int) = NearbyPlace(
    id = "nearby-$index-$placeName",
    title = placeName,
    address = region.orEmpty(),
    description = description.orEmpty(),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    priceWon = price,
)
