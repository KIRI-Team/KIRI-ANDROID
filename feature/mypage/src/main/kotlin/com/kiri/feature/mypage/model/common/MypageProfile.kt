package com.kiri.feature.mypage.model.common

data class MypageProfile(
    val nickname: String,
    val familyName: String,
    val role: String,
    // 서버 캐릭터 이미지. 없으면 역할별 기본 캐릭터, 역할도 없으면 기본 프로필 그림을 보여준다
    val imageUrl: String? = null,
) {
    companion object {
        // TODO: 사용자 정보 저장소가 생기면 실제 프로필로 교체 (현재는 Figma 예시 값)
        val Sample = MypageProfile(nickname = "쥬123", familyName = "끼리네가족", role = "딸")
    }
}
