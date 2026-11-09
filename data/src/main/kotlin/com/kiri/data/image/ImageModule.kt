package com.kiri.data.image

import com.kiri.domain.repository.ImageUploadRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object ImageApiModule {
    @Provides
    @Singleton
    fun provideImageApi(retrofit: Retrofit): ImageApi = retrofit.create(ImageApi::class.java)

    // 저장소(S3) 업로드 전용. 저장소는 Authorization 헤더를 받으면 거절하므로 토큰 처리를 붙이지 않는다
    // 사진 본문이 크고 서명이 담긴 주소를 남기지 않도록 로그도 남기지 않는다
    @Provides
    @Singleton
    @ImageUploadClient
    fun provideImageUploadClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class ImageUploadRepositoryModule {
    @Binds
    internal abstract fun bindImageUploadRepository(impl: ImageUploadRepositoryImpl): ImageUploadRepository
}

// 업로드 전용 OkHttpClient 를 구분한다
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ImageUploadClient
