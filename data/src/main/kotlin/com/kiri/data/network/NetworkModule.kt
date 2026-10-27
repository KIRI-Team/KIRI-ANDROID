package com.kiri.data.network

import com.kiri.data.BuildConfig
import com.kiri.data.auth.AuthInterceptor
import com.kiri.data.auth.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// 서버 통신에 쓰는 Json, OkHttpClient, Retrofit 을 앱 전체에서 하나씩 제공한다
// API 인터페이스는 기능별 모듈에서 retrofit.create(XxxApi::class.java) 로 만든다
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun provideJson(): Json = kiriJson

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, tokenAuthenticator: TokenAuthenticator): OkHttpClient =
        baseClientBuilder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor()) }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    // 토큰 재발급 전용. 재발급 요청이 401 을 받아도 다시 재발급하지 않도록 토큰 처리 없이 따로 만든다
    @Provides
    @Singleton
    @ReissueClient
    fun provideReissueRetrofit(json: Json): Retrofit = retrofit(
        client = baseClientBuilder()
            .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor()) }
            .build(),
        json = json,
    )

    private fun baseClientBuilder() = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(apiBaseUrl(BuildConfig.API_BASE_URL))
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // debug 빌드에서만 요청·응답 본문을 Logcat 에 남긴다. 토큰은 로그에 남기지 않는다
    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }
}

// 토큰 재발급 전용 Retrofit 을 구분한다
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ReissueClient

// 서버가 필드를 추가해도 앱이 깨지지 않도록 모르는 키는 무시하고, 기본값이 있는 필드는 생략을 허용한다
internal val kiriJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
