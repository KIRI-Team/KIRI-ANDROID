package com.kiri.data.di

import com.kiri.data.repository.FakeInviteRepository
import com.kiri.data.repository.InMemoryPendingInviteRepository
import com.kiri.domain.repository.InviteRepository
import com.kiri.domain.repository.PendingInviteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindInviteRepository(impl: FakeInviteRepository): InviteRepository

    @Binds
    abstract fun bindPendingInviteRepository(impl: InMemoryPendingInviteRepository): PendingInviteRepository
}
