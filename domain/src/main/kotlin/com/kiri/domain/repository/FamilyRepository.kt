package com.kiri.domain.repository

import com.kiri.domain.model.family.Family

interface FamilyRepository {
    // 가족 공간을 만들고, 함께 발급된 초대 코드·링크를 돌려준다
    // 같은 이름의 가족 공간이 이미 있으면 DuplicateFamilyNameException 으로 실패한다
    suspend fun createFamily(name: String): Result<Family>
}
