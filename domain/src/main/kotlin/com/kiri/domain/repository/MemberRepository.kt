package com.kiri.domain.repository

import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.member.MyInfo

interface MemberRepository {
    // 내 닉네임을 정한다. 다른 회원이 이미 쓰는 닉네임이면 DuplicateNicknameException 으로 실패한다
    suspend fun updateNickname(nickname: String): Result<Unit>

    // 가족 안에서의 내 역할을 정한다
    suspend fun updateFamilyRole(role: FamilyRole): Result<Unit>

    // 내 정보(닉네임, 역할, 가족명)를 불러온다. 가족이 없어도 조회할 수 있다
    suspend fun me(): Result<MyInfo> = Result.failure(UnsupportedOperationException())
}
