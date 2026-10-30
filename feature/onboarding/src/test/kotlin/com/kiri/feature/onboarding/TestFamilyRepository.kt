package com.kiri.feature.onboarding

import com.kiri.domain.model.family.Family
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.FamilyRepository

// 가족 공간 생성 결과를 정해두고 보낸 가족명을 모은다
class TestFamilyRepository(
    var result: Result<Family> = Result.success(
        Family(id = 1, name = "끼리네가족", invite = Invite(code = "AB12CD", url = "https://kiri.example.com/invite/AB12CD")),
    ),
) : FamilyRepository {
    val sentNames = mutableListOf<String>()

    override suspend fun createFamily(name: String): Result<Family> {
        sentNames += name
        return result
    }
}
