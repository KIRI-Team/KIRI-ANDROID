package com.kiri.data.place

import com.kiri.domain.place.PlaceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaceRepositoryModule {
    @Provides
    @Singleton
    internal fun providePlaceRepository(
        api: com.kiri.data.mission.MissionApi,
        missions: com.kiri.domain.repository.MissionInfoRepository,
        buckets: com.kiri.domain.repository.BucketItemRepository,
        json: kotlinx.serialization.json.Json,
    ): PlaceRepository = ServerPlaceRepository(api, missions, buckets, json)
}
