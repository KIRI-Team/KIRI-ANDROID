package com.kiri.data.family

import com.kiri.domain.repository.FamilyRepository
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
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class FamilyRepositoryModule {
    @Binds
    internal abstract fun bindFamilyRepository(impl: FamilyRepositoryImpl): FamilyRepository
}
