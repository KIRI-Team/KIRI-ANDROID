package com.kiri.data.repository

import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.invite.InviteLink
import com.kiri.domain.repository.InviteRepository
import javax.inject.Inject
import kotlin.random.Random

// TODO(invite-api): 초대 발급 API가 정해지면 서버 호출 구현으로 교체
// 가정: POST /relationships/invite → { code, url, expiresAt }
class FakeInviteRepository @Inject constructor() : InviteRepository {
    override suspend fun createInvite(): Result<Invite> {
        val code = generateCode()
        return Result.success(Invite(code = code, url = InviteLink.urlOf(code)))
    }

    private fun generateCode(): String =
        (1..CODE_LENGTH).map { CODE_CHARACTERS[Random.nextInt(CODE_CHARACTERS.length)] }.joinToString("")

    private companion object {
        // 가족코드 입력 화면과 같은 형식 (영문 대문자 + 숫자 6자리)
        const val CODE_LENGTH = 6
        const val CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }
}
