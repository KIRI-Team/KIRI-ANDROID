package com.kiri.data.family

import com.kiri.domain.repository.FamilyRepository
import com.kiri.domain.repository.InviteRepository
import com.kiri.domain.repository.PendingInviteRepository
import com.kiri.domain.usecase.invite.AcceptPendingInviteUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object FamilyApiModule {
    @Provides
    @Singleton
    fun provideFamilyApi(retrofit: Retrofit): FamilyApi = retrofit.create(FamilyApi::class.java)

    // domain 은 DI 에 의존하지 않으므로 UseCase 는 여기서 만들어 준다
    @Provides
    fun provideAcceptPendingInviteUseCase(
        pending: PendingInviteRepository,
        families: FamilyRepository,
    ): AcceptPendingInviteUseCase = AcceptPendingInviteUseCase(pending, families)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FamilyIdStoreModule {
    @Binds
    abstract fun bindFamilyIdStore(impl: DataStoreFamilyIdStore): FamilyIdStore
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class InviteRepositoryModule {
    @Binds
    internal abstract fun bindInviteRepository(impl: RemoteInviteRepository): InviteRepository
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class FamilyRepositoryModule {
    @Binds
    internal abstract fun bindFamilyRepository(impl: FamilyRepositoryImpl): FamilyRepository
}
