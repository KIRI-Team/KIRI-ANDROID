package com.kiri.domain.model.taste

// 내 취향. 이름을 그대로 서버 값으로 주고받는다
data class Taste(
    // 선호 활동 1~3개
    val categories: Set<TasteCategory>,
    val activity: TasteActivity,
    // 아래 항목은 null 이면 상관없음
    val space: TasteSpace? = null,
    val price: TastePrice? = null,
    val duration: TasteDuration? = null,
) {
    companion object {
        const val MAX_CATEGORIES = 3
    }
}

// 취향을 정한 적이 없으면 taste 가 null
data class MyTaste(
    val nickname: String,
    val taste: Taste?,
    val isSet: Boolean = taste != null,
)

enum class TasteCategory { EXHIBITION, POPUP, PERFORMANCE, FOOD_CAFE, OUTDOOR, EXPERIENCE }

enum class TasteSpace { INDOOR, OUTDOOR }

enum class TasteActivity { LIGHT, MODERATE, ACTIVE }

enum class TastePrice { FREE, LOW }

enum class TasteDuration { WITHIN_1H, H1_TO_3, HALF_DAY_PLUS }
