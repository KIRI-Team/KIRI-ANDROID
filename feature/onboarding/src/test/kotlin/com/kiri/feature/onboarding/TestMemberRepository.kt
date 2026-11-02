package com.kiri.feature.onboarding

import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.repository.MemberRepository

// 저장 결과를 정해두고 보낸 닉네임·역할을 모은다
class TestMemberRepository(
    var result: Result<Unit> = Result.success(Unit),
    var roleResult: Result<Unit> = Result.success(Unit),
) : MemberRepository {
    val sentNicknames = mutableListOf<String>()
    val sentRoles = mutableListOf<FamilyRole>()

    override suspend fun updateNickname(nickname: String): Result<Unit> {
        sentNicknames += nickname
        return result
    }

    override suspend fun updateFamilyRole(role: FamilyRole): Result<Unit> {
        sentRoles += role
        return roleResult
    }
}
