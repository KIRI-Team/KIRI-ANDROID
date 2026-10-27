package com.kiri.data.term

import com.kiri.domain.repository.TermRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object TermApiModule {
    @Provides
    @Singleton
    fun provideTermApi(retrofit: Retrofit): TermApi = retrofit.create(TermApi::class.java)
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class TermRepositoryModule {
    @Binds
    internal abstract fun bindTermRepository(impl: TermRepositoryImpl): TermRepository
}
