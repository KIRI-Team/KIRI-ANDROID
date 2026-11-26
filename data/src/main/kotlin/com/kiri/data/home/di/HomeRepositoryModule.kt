package com.kiri.data.home.di

import com.kiri.domain.home.repository.HomeRepository
import com.kiri.domain.home.repository.HomeTutorialRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.time.Clock
import com.kiri.data.home.remote.HomeApi
import com.kiri.data.home.repository.HomeTutorialRepositoryImpl
import com.kiri.data.home.repository.RemoteHomeRepository
import com.kiri.data.home.repository.UpcomingHomeRepository

@Module
@InstallIn(SingletonComponent::class)
object HomeRepositoryModule {
    @Provides
    fun provideHomeTutorialRepository(local: HomeTutorialRepositoryImpl): HomeTutorialRepository = local

    @Provides
    @Singleton
    internal fun provideHomeRepository(retrofit: retrofit2.Retrofit, tokens: com.kiri.data.auth.local.TokenStore, json: kotlinx.serialization.json.Json, clock: Clock, missions: com.kiri.data.mission.remote.MissionApi, familyIds: com.kiri.data.family.local.FamilyIdStore): HomeRepository {
        val api = retrofit.create(HomeApi::class.java)
        val families = retrofit.create(com.kiri.data.family.remote.FamilyApi::class.java)
        return UpcomingHomeRepository(RemoteHomeRepository(api, tokens, json, clock, missions, families, familyIds, loadRecommendations = true), api, json)
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
