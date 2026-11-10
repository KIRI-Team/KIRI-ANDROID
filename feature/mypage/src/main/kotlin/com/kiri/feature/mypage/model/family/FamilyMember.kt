package com.kiri.feature.mypage.model.family

import com.kiri.domain.home.FamilyHome

data class FamilyMember(
    // 역할 표시 이름 (엄마·아빠·딸·아들). 정하지 않았으면 빈 문자열
    val role: String,
    val nickname: String,
    // 내 카드에만 닉네임 수정(펜) 아이콘을 보여준다
    val isMe: Boolean = false,
    val id: String = "",
    // 서버 캐릭터 이미지. 없거나 불러오지 못하면 역할별 기본 캐릭터를 보여준다
    val imageUrl: String? = null,
)

// 가족 기본 설정 화면 상태 (가족 정보 API 로 채운다)
data class FamilySettingUiState(
    val loading: Boolean = true,
    // 소속된 가족이 없으면 null
    val familyName: String? = null,
    // 내 카드를 맨 앞에 둔다
    val members: List<FamilyMember> = emptyList(),
    val failed: Boolean = false,
) {
    val me: FamilyMember? get() = members.firstOrNull { it.isMe }
}

// 가족 정보를 화면 상태로 바꾼다. 내 카드를 맨 앞에 두고 나머지는 서버 순서대로 둔다
internal fun FamilyHome.toFamilySettingUiState(): FamilySettingUiState {
    val members = members.filter { it.accessible }.map { FamilyMember(it.role, it.nickname, isMe = it.id == currentUserId, id = it.id, imageUrl = it.imageUrl) }
    return FamilySettingUiState(
        loading = false,
        familyName = familyName,
        members = members.sortedByDescending { it.isMe },
    )
}
