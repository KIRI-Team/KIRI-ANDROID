package com.kiri.app.record

import com.kiri.data.bucket.BucketItemRepositoryModule
import com.kiri.data.bucket.FakeBucketItemRepository
import com.kiri.domain.repository.BucketItemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

// UI 테스트에서는 실제 서버 대신 정해 둔 가족 버킷리스트를 쓴다 (엄마 1, 아빠 2 / 버킷 12·11·10)
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [BucketItemRepositoryModule::class])
object FakeBucketItemModule {
    @Provides
    @Singleton
    fun provideBucketItemRepository(): BucketItemRepository = FakeBucketItemRepository()
}
