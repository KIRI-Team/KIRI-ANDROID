package com.kiri.feature.mypage.model.preference

// 내 취향 설정의 항목별 선택지. 화면에 보이는 순서대로 둔다
// TODO: 취향 API가 연결되면 서버 값(EXHIBITION, INDOOR 등)과 연결

// 선호 활동 (최대 3개)
enum class PreferenceCategory(val label: String) {
    EXHIBITION("전시·문화"),
    POPUP("팝업·행사"),
    PERFORMANCE("공연"),
    FOOD_CAFE("맛집·카페"),
    WALK_OUTDOOR("산책·야외"),
    EXPERIENCE("체험·놀거리"),
}

// 공간
enum class PreferenceSpace(val label: String) {
    INDOOR("실내"),
    ANY("상관없음"),
    OUTDOOR("야외"),
}

// 활동량
enum class PreferenceActivity(val label: String) {
    LIGHT("가볍게"),
    MODERATE("적당히"),
    ACTIVE("많이 움직여도 괜찮음"),
}

// 비용
enum class PreferencePrice(val label: String) {
    FREE("무료 위주"),
    LOW("부담 적게"),
    ANY("비용 상관없음"),
}

// 활동시간
enum class PreferenceDuration(val label: String) {
    WITHIN_1H("1시간 이내"),
    ONE_TO_THREE_HOURS("1-3시간"),
    HALF_DAY_OR_MORE("반나절이상"),
    ANY("상관없음"),
}
