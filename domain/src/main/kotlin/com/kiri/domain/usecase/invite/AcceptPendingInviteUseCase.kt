package com.kiri.domain.usecase.invite

import com.kiri.domain.model.family.AlreadyInFamilyException
import com.kiri.domain.model.family.ExpiredInviteCodeException
import com.kiri.domain.model.family.InvalidInviteCodeException
import com.kiri.domain.model.family.JoinedFamily
import com.kiri.domain.repository.FamilyRepository
import com.kiri.domain.repository.PendingInviteRepository
import java.io.IOException

// 초대 링크로 받아 둔 토큰이 있으면 그 가족에 참여한다.
// 온보딩(가족 선택 화면에 들어올 때)과 메인(화면에 돌아올 때)이 같은 흐름을 쓴다
class AcceptPendingInviteUseCase(
    private val pending: PendingInviteRepository,
    private val families: FamilyRepository,
) {
    // 참여했거나 다시 써도 소용없는 토큰(없는 가족·만료·이미 다른 가족·서버 거절)이면 토큰을 지운다.
    // 연결 문제로 실패했을 때만 토큰을 남겨 다음에 다시 시도한다
    suspend operator fun invoke(): InviteAcceptResult {
        val token = pending.get() ?: return InviteAcceptResult.NoInvite
        val result = families.joinFamilyByToken(token)
        val error = result.exceptionOrNull()
        if (error is IOException) return InviteAcceptResult.Failed(error)
        pending.clear()
        return result.fold(
            onSuccess = { InviteAcceptResult.Joined(it) },
            onFailure = { InviteAcceptResult.Failed(it) },
        )
    }
}

sealed interface InviteAcceptResult {
    // 받아 둔 초대가 없다
    data object NoInvite : InviteAcceptResult

    data class Joined(val family: JoinedFamily) : InviteAcceptResult

    data class Failed(val error: Throwable) : InviteAcceptResult {
        // 연결 문제면 토큰이 남아 있어 다음에 다시 시도한다
        val willRetry: Boolean get() = error is IOException
    }
}

// 결과를 사용자에게 알릴 문구. 받아 둔 초대가 없으면 null
val InviteAcceptResult.message: String?
    get() = when (this) {
        InviteAcceptResult.NoInvite -> null
        is InviteAcceptResult.Joined -> "${family.name}에 참여했어요."
        is InviteAcceptResult.Failed -> when (error) {
            is ExpiredInviteCodeException -> "만료된 초대 링크예요. 가족에게 새 링크를 받아주세요."
            is InvalidInviteCodeException -> "초대 링크를 확인할 수 없어요. 가족에게 링크를 다시 받아주세요."
            is AlreadyInFamilyException -> "이미 다른 가족에 가입되어 있어 초대 링크로 참여할 수 없어요."
            else -> "초대 링크로 가족에 참여하지 못했어요. 잠시 후 다시 시도해주세요."
        }
    }
