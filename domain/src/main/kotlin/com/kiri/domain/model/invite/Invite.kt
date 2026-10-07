package com.kiri.domain.model.invite

// 가족 초대 코드와 공유용 링크
data class Invite(
    val code: String,
    val url: String,
)
