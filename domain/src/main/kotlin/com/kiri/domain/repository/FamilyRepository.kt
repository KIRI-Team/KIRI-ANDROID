package com.kiri.domain.repository

import com.kiri.domain.model.family.Family

interface FamilyRepository {
    suspend fun getFamilyId(): Result<Long> = Result.failure(UnsupportedOperationException())
    suspend fun resolveAddress(postalCode: String, address: String, regionCode: String): Result<com.kiri.domain.model.family.FamilyAddress> = Result.failure(UnsupportedOperationException())
    suspend fun saveAddress(address: com.kiri.domain.model.family.FamilyAddress): Result<Unit> = Result.failure(UnsupportedOperationException())

    // 가족 공간을 만들고, 함께 발급된 초대 코드·링크를 돌려준다
    // 같은 이름의 가족 공간이 이미 있으면 DuplicateFamilyNameException 으로 실패한다
    suspend fun createFamily(name: String): Result<Family>

    // 내 가족 공간의 이름을 바꾸고 바뀐 이름을 돌려준다
    // 같은 이름의 가족 공간이 이미 있으면 DuplicateFamilyNameException, 이 기기에 가족 공간 정보가 없으면 NoFamilyException 으로 실패한다
    suspend fun renameFamily(name: String): Result<String>
}
