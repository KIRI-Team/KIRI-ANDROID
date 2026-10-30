package com.kiri.feature.mypage.model.nameedit

import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.domain.model.nickname.NicknameValidator

// 닉네임·가족명 수정 화면이 함께 쓰는 상태
data class NameEditUiState(
    // 입력창 안내 문구에 보여주는 지금 이름
    val currentName: String = "",
    val name: String = "",
    // 다음 버튼을 눌렀을 때 지키지 못한 규칙 (입력을 바꾸면 비운다)
    val violations: Set<NicknameRule> = emptySet(),
    // 닉네임은 12자, 가족명은 15자까지 허용한다
    val maxLength: Int = NicknameValidator.MAX_LENGTH,
)
