package com.kiri.data.place.di

import com.kiri.domain.place.repository.PlaceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.kiri.data.place.repository.PlaceRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
object PlaceRepositoryModule {
    @Provides
    @Singleton
    internal fun providePlaceRepository(
        api: com.kiri.data.mission.remote.MissionApi,
        missions: com.kiri.domain.mission.repository.MissionInfoRepository,
        buckets: com.kiri.domain.bucket.repository.BucketItemRepository,
        json: kotlinx.serialization.json.Json,
    ): PlaceRepository = PlaceRepositoryImpl(api, missions, buckets, json)
}
