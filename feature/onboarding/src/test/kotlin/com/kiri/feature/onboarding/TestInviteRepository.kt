package com.kiri.feature.onboarding

import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.InviteRepository

// 발급 결과를 정해두고 호출 횟수를 센다
class TestInviteRepository(
    var result: Result<Invite> = Result.success(Invite(code = "AB12CD", url = "https://kiri.example.com/invite/AB12CD")),
) : InviteRepository {
    var createCount = 0
        private set

    override suspend fun createInvite(): Result<Invite> {
        createCount++
        return result
    }

    override suspend fun getCurrentInvite(): Invite? = result.getOrNull()?.takeIf { createCount > 0 }
}
