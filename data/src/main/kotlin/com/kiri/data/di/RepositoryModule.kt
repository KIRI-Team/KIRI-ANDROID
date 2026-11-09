package com.kiri.data.di

import com.kiri.data.repository.DataStorePendingInviteRepository
import com.kiri.domain.repository.PendingInviteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// 초대 저장소(InviteRepository)는 UI 테스트에서 바꿀 수 있도록 family/FamilyModule 의 InviteRepositoryModule 에 둔다
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindPendingInviteRepository(impl: DataStorePendingInviteRepository): PendingInviteRepository
}
