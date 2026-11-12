package com.kiri.feature.onboarding

import com.kiri.domain.model.family.Family
import com.kiri.domain.model.family.JoinedFamily
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.FamilyRepository

// 가족 공간 생성 결과를 정해두고 보낸 가족명을 모은다
class TestFamilyRepository(
    var result: Result<Family> = Result.success(
        Family(id = 1, name = "끼리네가족", invite = Invite(code = "AB12CD", url = "https://kiri.app/invite/AB12CD")),
    ),
) : FamilyRepository {
    val sentNames = mutableListOf<String>()

    // 초대 코드 가입 결과와 보낸 코드
    var joinResult: Result<JoinedFamily> = Result.success(JoinedFamily(9, "끼리네가족"))
    val sentCodes = mutableListOf<String>()

    override suspend fun joinFamily(inviteCode: String): Result<JoinedFamily> {
        sentCodes += inviteCode
        return joinResult
    }

    // 초대 토큰 가입 결과와 보낸 토큰
    var tokenJoinResult: Result<JoinedFamily> = Result.success(JoinedFamily(9, "끼리네가족"))
    val sentTokens = mutableListOf<String>()

    override suspend fun joinFamilyByToken(inviteToken: String): Result<JoinedFamily> {
        sentTokens += inviteToken
        return tokenJoinResult
    }

    override suspend fun createFamily(name: String): Result<Family> {
        sentNames += name
        return result
    }

    override suspend fun renameFamily(name: String): Result<String> = Result.success(name)
}
