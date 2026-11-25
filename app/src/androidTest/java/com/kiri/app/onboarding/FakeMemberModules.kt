package com.kiri.app.onboarding

import com.kiri.data.family.FamilyRepositoryModule
import com.kiri.data.family.InviteRepositoryModule
import com.kiri.data.member.MemberRepositoryModule
import com.kiri.data.repository.FakeInviteRepository
import com.kiri.domain.invite.repository.InviteRepository
import javax.inject.Singleton
import com.kiri.domain.family.model.Family
import com.kiri.domain.invite.model.Invite
import com.kiri.domain.member.model.FamilyRole
import com.kiri.domain.family.repository.FamilyRepository
import com.kiri.domain.member.repository.MemberRepository
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
        Family(id = 1, name = name, invite = Invite(code = "AB12CD", url = "https://halo-official.co.kr/invite/AB12CD")),
    )

    override suspend fun renameFamily(name: String): Result<String> = Result.success(name)

    // 아직 우리 집 주소를 등록하지 않은 가족
    override suspend fun getAddress(): Result<com.kiri.domain.family.model.FamilyAddress?> = Result.success(null)
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

// UI 테스트에서는 서버 대신 메모리에서 초대를 발급·기억하는 가짜 저장소를 쓴다 (화면 사이에서 같은 초대를 보도록 하나만 만든다)
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [InviteRepositoryModule::class])
object FakeInviteRepositoryModule {
    @Provides
    @Singleton
    fun provideInviteRepository(): InviteRepository = FakeInviteRepository()
}
