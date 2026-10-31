package com.kiri.feature.mypage

import com.kiri.domain.model.family.Family
import com.kiri.domain.repository.FamilyRepository

// 가족명 변경 결과를 정해두고 보낸 가족명을 모은다
class TestFamilyRepository(
    // 정하면 변경 대신 이 실패를 돌려준다
    var renameError: Throwable? = null,
) : FamilyRepository {
    val renamedNames = mutableListOf<String>()

    override suspend fun createFamily(name: String): Result<Family> = error("마이페이지에서는 가족 공간을 만들지 않는다")

    override suspend fun renameFamily(name: String): Result<String> {
        renamedNames += name
        renameError?.let { return Result.failure(it) }
        return Result.success(name)
    }
}
