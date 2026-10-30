package com.kiri.app.onboarding

import com.kiri.data.family.FamilyRepositoryModule
import com.kiri.data.member.MemberRepositoryModule
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.repository.FamilyRepository
import com.kiri.domain.repository.MemberRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 실제 서버 대신 닉네임·역할이 바로 저장된 것으로 본다
class FakeMemberRepository : MemberRepository {
    override suspend fun updateNickname(nickname: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateFamilyRole(role: FamilyRole): Result<Unit> = Result.success(Unit)
}

// UI 테스트에서는 실제 서버 대신 가족 공간이 바로 만들어지고 고정된 초대 코드를 받은 것으로 본다
class FakeFamilyRepository : FamilyRepository {
    override suspend fun createFamily(name: String): Result<Family> = Result.success(
        Family(id = 1, name = name, invite = Invite(code = "AB12CD", url = "https://kiri.example.com/invite/AB12CD")),
    )
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [MemberRepositoryModule::class])
object FakeMemberRepositoryModule {
    @Provides
    fun provideMemberRepository(): MemberRepository = FakeMemberRepository()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [FamilyRepositoryModule::class])
object FakeFamilyRepositoryModule {
    @Provides
    fun provideFamilyRepository(): FamilyRepository = FakeFamilyRepository()
}
