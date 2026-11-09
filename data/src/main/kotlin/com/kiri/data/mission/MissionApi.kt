package com.kiri.data.mission

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.mission.MissionBucketState
import com.kiri.domain.model.mission.MissionGuide
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.model.mission.MissionPlace
import com.kiri.domain.model.mission.MissionStatus
import com.kiri.domain.model.mission.WalkingLevel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

internal interface MissionApi {
    @retrofit2.http.POST("api/v1/missions/{missionId}/bucket")
    suspend fun saveBucket(@Path("missionId") missionId: Long): ApiResponse<kotlinx.serialization.json.JsonElement>

    @GET("api/v1/missions")
    suspend fun getFeed(@QueryMap filters: Map<String, String>): ApiResponse<MissionFeedResponse>

    @GET("api/v1/missions/recommendations")
    suspend fun getWeeklyRecommendation(): ApiResponse<WeeklyRecommendationResponse>

    // 로그인 토큰이 필요하다. 없거나 노출할 수 없는 미션이면 MISSION404_1, id 형식이 틀리면 COMMON400_1 로 거절된다
    // TODO: 받은 경로는 mission/{missionId} 였다. 다른 API 와 같이 api/v1 과 복수형을 붙였으니 서버 명세와 대조할 것
    @GET("api/v1/missions/{missionId}")
    suspend fun getMission(@Path("missionId") missionId: Long): ApiResponse<MissionResponse>
}

@Serializable
internal data class MissionFeedResponse(
    val missions: List<MissionFeedItem>,
    val nextCursor: Long? = null,
    val hasNext: Boolean,
)

@Serializable
internal data class MissionFeedItem(
    val missionId: Long,
    val title: String,
    val category: String,
    val placeName: String? = null,
    val imageUrl: String? = null,
    val imageSource: String? = null,
    val price: Int? = null,
    val distance: Double? = null,
)

@Serializable
internal data class WeeklyRecommendationResponse(
    val missionId: Long,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val recommendationReason: String? = null,
)

// 날짜는 "2026-11-17", 시간은 "10:00" 처럼 온다. 앱이 모르는 값이 와도 상세는 보여줄 수 있도록 상태 값은 문자열로 받는다
@Serializable
internal data class MissionResponse(
    val missionId: Long,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val missionType: String? = null,
    val status: String? = null,
    val indoorOutdoor: String? = null,
    val imageUrl: String? = null,
    val imageSource: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val openingTime: String? = null,
    val closingTime: String? = null,
    val price: Int? = null,
    val durationMinMinutes: Int? = null,
    val durationMaxMinutes: Int? = null,
    val walkingLevel: String? = null,
    val sourceUrl: String? = null,
    val place: MissionPlaceResponse? = null,
    val prepareItems: List<MissionGuideResponse> = emptyList(),
    val steps: List<MissionGuideResponse> = emptyList(),
    val bucketState: String? = null,
    val averageRating: Double? = null,
    val reviewCount: Int = 0,
)

@Serializable
internal data class MissionPlaceResponse(
    val placeId: Long,
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
internal data class MissionGuideResponse(
    val title: String,
    val description: String? = null,
)

// 날짜·시간을 읽지 못하면 그 값만 비우고 나머지는 보여준다
internal fun MissionResponse.toMissionInfo() = MissionInfo(
    id = missionId,
    title = title,
    description = description,
    category = category,
    missionType = missionType,
    status = MissionStatus.entries.firstOrNull { it.name == status },
    indoorOutdoor = indoorOutdoor,
    imageUrl = imageUrl,
    imageSource = imageSource?.takeIf { imageUrl != null },
    startDate = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    endDate = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    openingTime = openingTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    closingTime = closingTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    price = price,
    durationMinMinutes = durationMinMinutes,
    durationMaxMinutes = durationMaxMinutes,
    walkingLevel = WalkingLevel.entries.firstOrNull { it.name == walkingLevel },
    sourceUrl = sourceUrl,
    place = place?.let { MissionPlace(it.placeId, it.name, it.address, it.latitude, it.longitude) },
    prepareItems = prepareItems.map { MissionGuide(it.title, it.description) },
    steps = steps.map { MissionGuide(it.title, it.description) },
    bucketState = MissionBucketState.entries.firstOrNull { it.name == bucketState } ?: MissionBucketState.NONE,
    averageRating = averageRating,
    reviewCount = reviewCount,
)
