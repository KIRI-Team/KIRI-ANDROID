package com.kiri.data.member

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiCommand
import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.nickname.DuplicateNicknameException
import com.kiri.domain.repository.MemberRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class MemberRepositoryImpl @Inject constructor(
    private val api: MemberApi,
    private val json: Json,
    private val session: com.kiri.domain.repository.SessionRepository? = null,
) : MemberRepository {

    // 닉네임 중복은 화면에서 따로 안내해야 해서 도메인 오류로 바꾼다. 나머지 실패는 그대로 돌려준다
    override suspend fun updateNickname(nickname: String): Result<Unit> =
        apiCommand(json) { api.updateNickname(NicknameRequest(nickname)) }.onSuccess { session?.saveNickname(nickname) }
            .recoverCatching { error ->
                if (error is KiriApiException && error.code == DUPLICATE_NICKNAME_CODE) throw DuplicateNicknameException()
                throw error
            }

    override suspend fun updateFamilyRole(role: FamilyRole): Result<Unit> =
        apiCommand(json) { api.updateFamilyRole(FamilyRoleRequest(role.name)) }.onSuccess { session?.saveRole(role.name) }

    private companion object {
        const val DUPLICATE_NICKNAME_CODE = "MEMBER409_1"
    }
}
