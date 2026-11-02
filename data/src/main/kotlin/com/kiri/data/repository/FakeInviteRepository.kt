package com.kiri.data.repository

import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.invite.InviteLink
import com.kiri.domain.repository.InviteRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// UI 테스트에서 실제 서버 대신 쓰는 초대 저장소 (앱은 family/RemoteInviteRepository 를 쓴다)
// 발급한 초대를 지금 초대로 기억한다
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

    override suspend fun getCurrentInvite(): Result<Invite> =
        current?.let { Result.success(it) } ?: Result.failure(NoFamilyException())

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
