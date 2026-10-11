package com.kiri.feature.mypage

import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.InviteRepository

// 지금 초대와 발급 결과를 정해두고 발급 횟수를 센다
class TestInviteRepository(
    var current: Invite? = null,
    var result: Result<Invite> = Result.success(NEW_INVITE),
) : InviteRepository {
    var createCount = 0
        private set

    override suspend fun createInvite(): Result<Invite> {
        createCount++
        result.onSuccess { current = it }
        return result
    }

    override suspend fun getCurrentInvite(): Invite? = current

    companion object {
        val NEW_INVITE = Invite(code = "NEW123", url = "https://kiri.example.com/invite/NEW123")
    }
}
