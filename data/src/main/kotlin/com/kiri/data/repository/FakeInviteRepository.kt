package com.kiri.data.repository

import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.invite.InviteLink
import com.kiri.domain.repository.InviteRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// TODO(invite-api): 초대 발급 API가 정해지면 서버 호출 구현으로 교체
// 가정: POST /relationships/invite → { code, url, expiresAt }
// 온보딩에서 발급한 초대를 마이페이지에서도 보여줄 수 있도록 앱 전체에서 하나만 두고 지금 초대를 기억한다
@Singleton
class FakeInviteRepository(
    private val clock: Clock,
) : InviteRepository {
    @Inject
    constructor() : this(Clock.system(ZoneId.of("Asia/Seoul")))

    private var current: Invite? = null

    override suspend fun createInvite(): Result<Invite> {
        val code = generateCode()
        val invite = Invite(
            code = code,
            url = InviteLink.urlOf(code),
            expiresAt = LocalDate.now(clock).plusDays(VALID_DAYS),
        )
        current = invite
        return Result.success(invite)
    }

    override suspend fun getCurrentInvite(): Invite? = current

    private fun generateCode(): String =
        (1..CODE_LENGTH).map { CODE_CHARACTERS[Random.nextInt(CODE_CHARACTERS.length)] }.joinToString("")

    private companion object {
        // 가족코드 입력 화면과 같은 형식 (영문 대문자 + 숫자 6자리)
        const val CODE_LENGTH = 6
        const val CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        // 발급한 날부터 7일 동안 쓸 수 있다 (실제 기간은 API가 정한다)
        const val VALID_DAYS = 7L
    }
}
