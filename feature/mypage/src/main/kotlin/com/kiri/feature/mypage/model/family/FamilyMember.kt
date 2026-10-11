package com.kiri.feature.mypage.model.family

data class FamilyMember(
    val role: String,
    val nickname: String,
    // 내 카드에만 닉네임 수정(펜) 아이콘을 보여준다
    val isMe: Boolean = false,
) {
    companion object {
        // TODO: 가족 정보 저장소가 생기면 실제 구성원으로 교체 (현재는 Figma 예시 값)
        val Samples = listOf(
            FamilyMember(role = "딸", nickname = "쥬123", isMe = true),
            FamilyMember(role = "아들", nickname = "첫째"),
            FamilyMember(role = "엄마", nickname = "마마"),
            FamilyMember(role = "아들", nickname = "첫째"),
            FamilyMember(role = "아들", nickname = "첫째"),
            FamilyMember(role = "엄마", nickname = "마마"),
        )
    }
}
