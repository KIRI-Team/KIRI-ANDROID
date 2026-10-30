package com.kiri.domain.repository

import com.kiri.domain.model.member.FamilyRole

interface MemberRepository {
    // 내 닉네임을 정한다. 다른 회원이 이미 쓰는 닉네임이면 DuplicateNicknameException 으로 실패한다
    suspend fun updateNickname(nickname: String): Result<Unit>

    // 가족 안에서의 내 역할을 정한다
    suspend fun updateFamilyRole(role: FamilyRole): Result<Unit>
}
