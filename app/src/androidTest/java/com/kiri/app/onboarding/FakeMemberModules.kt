package com.kiri.app.onboarding

import com.kiri.data.member.MemberRepositoryModule
import com.kiri.domain.model.member.FamilyRole
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

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [MemberRepositoryModule::class])
object FakeMemberRepositoryModule {
    @Provides
    fun provideMemberRepository(): MemberRepository = FakeMemberRepository()
}
