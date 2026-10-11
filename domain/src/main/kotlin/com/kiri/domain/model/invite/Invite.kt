package com.kiri.domain.model.invite

import java.time.LocalDate

// 가족 초대 코드와 공유용 링크
data class Invite(
    val code: String,
    val url: String,
    // 이 날까지 초대를 쓸 수 있다 (null이면 만료일 없음)
    val expiresAt: LocalDate? = null,
)
