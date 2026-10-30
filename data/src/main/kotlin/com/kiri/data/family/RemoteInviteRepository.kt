package com.kiri.data.family

import com.kiri.data.network.ApiResponse
import com.kiri.data.network.apiResult
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.InviteRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

// 내 가족 공간의 초대를 서버에서 불러오고 재발급한다. 가족 ID는 가족 공간을 만들 때 기억해 둔 값을 쓴다
internal class RemoteInviteRepository @Inject constructor(
    private val api: FamilyApi,
    private val familyIdStore: FamilyIdStore,
    private val json: Json,
) : InviteRepository {

    override suspend fun createInvite(): Result<Invite> = withFamilyId { api.reissueInvite(it) }

    override suspend fun getCurrentInvite(): Result<Invite> = withFamilyId { api.getInvite(it) }

    // 가족 ID가 없으면 요청하지 않고 실패한다
    private suspend fun withFamilyId(call: suspend (Long) -> ApiResponse<InviteResponse>): Result<Invite> {
        val familyId = familyIdStore.get() ?: return Result.failure(NoFamilyException())
        return apiResult(json) { call(familyId) }.map { it.toInvite() }
    }
}
