package com.kiri.feature.mypage.model.preference

import com.kiri.domain.model.taste.TasteActivity
import com.kiri.domain.model.taste.TasteCategory
import com.kiri.domain.model.taste.TasteDuration
import com.kiri.domain.model.taste.TastePrice
import com.kiri.domain.model.taste.TasteSpace

// 내 취향 설정의 항목별 선택지. 화면에 보이는 순서대로 두고, 각 선택지가 뜻하는 취향 값을 함께 둔다
// "상관없음" 선택지는 취향 값이 null 이다

// 선호 활동 (1~3개)
enum class PreferenceCategory(val label: String, val taste: TasteCategory) {
    EXHIBITION("전시·문화", TasteCategory.EXHIBITION),
    POPUP("팝업·행사", TasteCategory.POPUP),
    PERFORMANCE("공연", TasteCategory.PERFORMANCE),
    FOOD_CAFE("맛집·카페", TasteCategory.FOOD_CAFE),
    OUTDOOR("산책·야외", TasteCategory.OUTDOOR),
    EXPERIENCE("체험·놀거리", TasteCategory.EXPERIENCE),
}

// 공간
enum class PreferenceSpace(val label: String, val taste: TasteSpace?) {
    INDOOR("실내", TasteSpace.INDOOR),
    ANY("상관없음", null),
    OUTDOOR("야외", TasteSpace.OUTDOOR),
}

// 활동량 (필수)
enum class PreferenceActivity(val label: String, val taste: TasteActivity) {
    LIGHT("가볍게", TasteActivity.LIGHT),
    MODERATE("적당히", TasteActivity.MODERATE),
    ACTIVE("많이 움직여도 괜찮음", TasteActivity.ACTIVE),
}

// 비용
enum class PreferencePrice(val label: String, val taste: TastePrice?) {
    FREE("무료 위주", TastePrice.FREE),
    LOW("부담 적게", TastePrice.LOW),
    ANY("비용 상관없음", null),
}

// 활동시간
enum class PreferenceDuration(val label: String, val taste: TasteDuration?) {
    WITHIN_1H("1시간 이내", TasteDuration.WITHIN_1H),
    ONE_TO_THREE_HOURS("1-3시간", TasteDuration.H1_TO_3),
    HALF_DAY_OR_MORE("반나절이상", TasteDuration.HALF_DAY_PLUS),
    ANY("상관없음", null),
}
