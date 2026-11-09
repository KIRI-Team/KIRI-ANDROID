package com.kiri.data.family

import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.invite.InviteReissueFailedException
import com.kiri.domain.repository.InviteRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

// 내 가족 공간의 초대를 서버에서 불러오고 재발급한다. 가족 ID는 기억해 둔 값을, 없으면 가족 ID 조회 API 로 받아 쓴다
internal class RemoteInviteRepository @Inject constructor(
    private val api: FamilyApi,
    private val familyIdStore: FamilyIdStore,
    private val json: Json,
) : InviteRepository {

    // 지금 초대를 지운 뒤 새로 발급한다. 지우지 못하면 발급하지 않고 실패한다
    // 지운 뒤 발급하지 못하면 이전 초대를 더 쓸 수 없으니 InviteReissueFailedException 으로 알린다
    override suspend fun createInvite(): Result<Invite> = withFamilyId { familyId ->
        apiCommand(json) { api.deleteInvite(familyId) }.onFailure { return@withFamilyId Result.failure(it) }
        apiResult(json) { api.reissueInvite(familyId) }
            .onFailure { return@withFamilyId Result.failure(InviteReissueFailedException(it)) }
    }

    override suspend fun getCurrentInvite(): Result<Invite> = withFamilyId { apiResult(json) { api.getInvite(it) } }

    // 소속된 가족이 없으면 초대를 요청하지 않고 실패한다
    private suspend fun withFamilyId(call: suspend (Long) -> Result<InviteResponse>): Result<Invite> {
        val familyId = familyIdStore.resolve(api, json).getOrElse { return Result.failure(it) }
        return call(familyId).map { it.toInvite() }
    }
}
