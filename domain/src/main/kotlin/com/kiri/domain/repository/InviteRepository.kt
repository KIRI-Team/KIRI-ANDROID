package com.kiri.domain.repository

import com.kiri.domain.model.invite.Invite

interface InviteRepository {
    // 가족 초대 코드와 링크를 발급한다
    suspend fun createInvite(): Result<Invite>
}
