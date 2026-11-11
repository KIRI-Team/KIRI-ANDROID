package com.kiri.data.taste

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.taste.MyTaste
import com.kiri.domain.model.taste.Taste
import com.kiri.domain.model.taste.TasteActivity
import com.kiri.domain.model.taste.TasteCategory
import com.kiri.domain.model.taste.TasteDuration
import com.kiri.domain.model.taste.TastePrice
import com.kiri.domain.model.taste.TasteSpace
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

internal interface TasteApi {
    // 로그인 토큰이 필요하다. 취향을 정한 적이 없으면 isSet 이 false 로 온다
    @GET("api/v1/members/taste")
    suspend fun getMyTaste(): ApiResponse<TasteResponse>

    // 로그인 토큰이 필요하다. 선호 활동이 1~3개가 아니면 COMMON400_1 로 거절된다
    @PUT("api/v1/members/taste")
    suspend fun saveMyTaste(@Body request: TasteRequest): ApiResponse<TasteResponse>
}

// null 인 항목(상관없음)은 보내지 않는다 (kiriJson 의 explicitNulls = false)
@Serializable
internal data class TasteRequest(
    val categories: List<String>,
    val indoorOutdoor: String? = null,
    val activityLevel: String,
    val priceLevel: String? = null,
    val durationLevel: String? = null,
)

internal fun Taste.toRequest() = TasteRequest(
    categories = categories.map { it.name },
    indoorOutdoor = space?.name,
    activityLevel = activity.name,
    priceLevel = price?.name,
    durationLevel = duration?.name,
)

// 서버가 앱이 모르는 값을 새로 보내도 화면을 열 수 있도록 문자열로 받는다
@Serializable
internal data class TasteResponse(
    val nickname: String = "",
    val isSet: Boolean = false,
    val categories: List<String>? = null,
    val indoorOutdoor: String? = null,
    val activityLevel: String? = null,
    val priceLevel: String? = null,
    val durationLevel: String? = null,
)

// 모르는 값은 선택값만 비운다. 설정 완료 여부는 서버의 isSet을 유지한다.
internal fun TasteResponse.toMyTaste(): MyTaste {
    val activity = activityLevel.toEnumOrNull<TasteActivity>()
    val taste = if (isSet && activity != null) {
        Taste(
            categories = categories.orEmpty().mapNotNull { it.toEnumOrNull<TasteCategory>() }.toSet(),
            activity = activity,
            space = indoorOutdoor.toEnumOrNull<TasteSpace>(),
            price = priceLevel.toEnumOrNull<TastePrice>(),
            duration = durationLevel.toEnumOrNull<TasteDuration>(),
        )
    } else {
        null
    }
    return MyTaste(nickname = nickname, taste = taste, isSet = isSet)
}

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
