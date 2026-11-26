package com.kiri.data.taste.di

import com.kiri.domain.taste.repository.TasteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import com.kiri.data.taste.remote.TasteApi
import com.kiri.data.taste.repository.TasteRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
internal object TasteApiModule {
    @Provides
    @Singleton
    fun provideTasteApi(retrofit: Retrofit): TasteApi = retrofit.create(TasteApi::class.java)
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class TasteRepositoryModule {
    @Binds
    internal abstract fun bindTasteRepository(impl: TasteRepositoryImpl): TasteRepository
}
