package com.kiri.data.home

import com.kiri.domain.home.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object HomeRepositoryModule {
    @Provides
    @Singleton
    fun provideHomeRepository(): HomeRepository = FakeHomeRepository()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
