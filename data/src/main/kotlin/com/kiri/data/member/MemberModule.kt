package com.kiri.data.member

import com.kiri.domain.repository.MemberRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object MemberApiModule {
    @Provides
    @Singleton
    fun provideMemberApi(retrofit: Retrofit): MemberApi = retrofit.create(MemberApi::class.java)
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class MemberRepositoryModule {
    @Binds
    internal abstract fun bindMemberRepository(impl: MemberRepositoryImpl): MemberRepository
}
