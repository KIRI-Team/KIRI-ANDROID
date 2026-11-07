package com.kiri.data.home

import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.HomeTutorialRepository
import com.kiri.domain.mission.MissionRepository
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
    fun provideHomeTutorialRepository(local: LocalHomeTutorialRepository): HomeTutorialRepository = local

    @Provides
    @Singleton
    internal fun provideHomeRepository(retrofit: retrofit2.Retrofit, tokens: com.kiri.data.auth.TokenStore, json: kotlinx.serialization.json.Json, clock: Clock, missions: com.kiri.data.mission.MissionApi): HomeRepository {
        val api = retrofit.create(HomeApi::class.java)
        return UpcomingHomeRepository(RemoteHomeRepository(api, tokens, json, clock, missions), api, json)
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
