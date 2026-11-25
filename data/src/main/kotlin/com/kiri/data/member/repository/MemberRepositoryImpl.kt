package com.kiri.data.member.repository

import com.kiri.data.common.network.KiriApiException
import com.kiri.data.common.network.apiCommand
import com.kiri.domain.member.model.FamilyRole
import com.kiri.domain.member.model.MyInfo
import com.kiri.data.common.network.apiResult
import com.kiri.domain.member.model.DuplicateNicknameException
import com.kiri.domain.member.repository.MemberRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.kiri.data.member.remote.FamilyRoleRequest
import com.kiri.data.member.remote.MemberApi
import com.kiri.data.member.remote.NicknameRequest

internal class MemberRepositoryImpl @Inject constructor(
    private val api: MemberApi,
    private val json: Json,
    private val session: com.kiri.domain.auth.repository.SessionRepository? = null,
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

    override suspend fun me(): Result<MyInfo> =
        apiResult(json) { api.me() }.map { response ->
            MyInfo(
                memberId = response.memberId,
                nickname = response.nickname,
                role = FamilyRole.entries.firstOrNull { it.name == response.familyRole },
                characterImageUrl = response.characterImageUrl,
                familyName = response.familyName,
            )
        }

    private companion object {
        const val DUPLICATE_NICKNAME_CODE = "MEMBER409_1"
    }
}
