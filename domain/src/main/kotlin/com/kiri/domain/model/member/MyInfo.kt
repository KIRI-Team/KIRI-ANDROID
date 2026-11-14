package com.kiri.domain.model.member

// 내 정보 (GET api/v1/members/me). 가족이 없는 둘러보기 회원도 조회할 수 있다
data class MyInfo(
    val memberId: Long,
    val nickname: String,
    // 아직 정하지 않았거나 앱이 모르는 값이면 null
    val role: FamilyRole?,
    // 역할별 KIRI 캐릭터 이미지. 역할이 없으면 null
    val characterImageUrl: String?,
    // 가족에 소속되지 않았으면 null
    val familyName: String?,
)
