package com.kiri.feature.mypage.model.common

data class MypageProfile(
    val nickname: String,
    val familyName: String,
    val role: String,
) {
    companion object {
        // TODO: 사용자 정보 저장소가 생기면 실제 프로필로 교체 (현재는 Figma 예시 값)
        val Sample = MypageProfile(nickname = "쥬123", familyName = "끼리네가족", role = "딸")
    }
}
