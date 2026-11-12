package com.kiri.domain.repository

import com.kiri.domain.model.family.Family

interface FamilyRepository {
    suspend fun getFamilyId(): Result<Long> = Result.failure(UnsupportedOperationException())
    suspend fun resolveAddress(postalCode: String, address: String, regionCode: String): Result<com.kiri.domain.model.family.FamilyAddress> = Result.failure(UnsupportedOperationException())
    suspend fun saveAddress(address: com.kiri.domain.model.family.FamilyAddress): Result<Unit> = Result.failure(UnsupportedOperationException())

    // 가족 공간을 만들고, 함께 발급된 초대 코드·링크를 돌려준다
    // 같은 이름의 가족 공간이 이미 있으면 DuplicateFamilyNameException 으로 실패한다
    suspend fun createFamily(name: String): Result<Family>

    // 초대 코드로 가족에 가입하고 가입한 가족을 돌려준다. 가입하면 그 가족 ID 를 기억한다
    // 코드가 틀렸거나 가족을 찾을 수 없으면 InvalidInviteCodeException, 만료됐으면 ExpiredInviteCodeException,
    // 이미 다른 가족에 있으면 AlreadyInFamilyException 으로 실패한다
    suspend fun joinFamily(inviteCode: String): Result<com.kiri.domain.model.family.JoinedFamily> =
        Result.failure(UnsupportedOperationException())

    // 초대 링크의 토큰으로 가족에 가입하고 가입한 가족을 돌려준다. 가입하면 그 가족 ID 를 기억한다
    // 토큰에 맞는 가족이 없으면 InvalidInviteCodeException, 만료됐으면 ExpiredInviteCodeException,
    // 이미 다른 가족에 있으면 AlreadyInFamilyException 으로 실패한다
    suspend fun joinFamilyByToken(inviteToken: String): Result<com.kiri.domain.model.family.JoinedFamily> =
        Result.failure(UnsupportedOperationException())

    // 내 가족 공간의 이름을 바꾸고 바뀐 이름을 돌려준다
    // 같은 이름의 가족 공간이 이미 있으면 DuplicateFamilyNameException, 이 기기에 가족 공간 정보가 없으면 NoFamilyException,
    // 방장이 아니면 NotFamilyOwnerException 으로 실패한다
    suspend fun renameFamily(name: String): Result<String>
}
