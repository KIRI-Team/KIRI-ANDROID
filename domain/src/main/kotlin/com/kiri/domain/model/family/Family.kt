package com.kiri.domain.model.family

import com.kiri.domain.model.invite.Invite

// 새로 만든 가족 공간과, 만들 때 함께 발급된 초대
data class Family(
    val id: Long,
    val name: String,
    val invite: Invite,
)
