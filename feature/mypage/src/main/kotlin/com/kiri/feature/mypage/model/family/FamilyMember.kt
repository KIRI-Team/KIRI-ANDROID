package com.kiri.feature.mypage.model.family

import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.member.MyInfo
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
    // 방장의 회원 id (모르면 null)
    val ownerId: String? = null,
    // 내 정보 조회(GET api/v1/members/me). 가족이 없는 둘러보기 회원의 프로필에 쓴다. 불러오지 못하면 null
    val myInfo: MyInfo? = null,
) {
    val me: FamilyMember? get() = members.firstOrNull { it.isMe }

    // 가족 정보를 불러왔는데 소속된 가족이 없다 = 둘러보기 중 (불러오는 중이거나 실패하면 둘러보기로 보지 않는다)
    val isBrowsing: Boolean get() = !loading && !failed && familyName == null

    // 가족명은 방장만 바꿀 수 있다. 방장을 모르면 막지 않고 서버 판단에 맡긴다
    val canRenameFamily: Boolean get() = familyName != null && (ownerId == null || me?.id == ownerId)
}

// 마이페이지에 보여줄 역할 이름
internal val FamilyRole.label: String
    get() = when (this) {
        FamilyRole.MOTHER -> "엄마"
        FamilyRole.FATHER -> "아빠"
        FamilyRole.DAUGHTER -> "딸"
        FamilyRole.SON -> "아들"
    }

// 가족 정보를 화면 상태로 바꾼다. 내 카드를 맨 앞에 두고 나머지는 서버 순서대로 둔다
internal fun FamilyHome.toFamilySettingUiState(): FamilySettingUiState {
    val members = members.filter { it.accessible }.map { FamilyMember(it.role, it.nickname, isMe = it.id == currentUserId, id = it.id, imageUrl = it.imageUrl) }
    return FamilySettingUiState(
        loading = false,
        familyName = familyName,
        members = members.sortedByDescending { it.isMe },
        ownerId = ownerId,
    )
}
