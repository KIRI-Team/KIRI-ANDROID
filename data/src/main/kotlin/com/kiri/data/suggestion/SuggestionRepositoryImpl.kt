package com.kiri.data.suggestion

import com.kiri.data.network.apiResult
import com.kiri.domain.repository.SuggestionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.Retrofit

internal class SuggestionRepositoryImpl @Inject constructor(
    private val api: SuggestionApi,
    private val json: Json,
) : SuggestionRepository {
    override suspend fun send(content: String, imageKey: String?): Result<Long> =
        apiResult(json) { api.send(SuggestionRequest(content, imageKey?.takeIf(String::isNotBlank))) }
            .map { it.suggestionId }
}

@Module
@InstallIn(SingletonComponent::class)
internal object SuggestionApiModule {
    @Provides
    @Singleton
    fun provideSuggestionApi(retrofit: Retrofit): SuggestionApi = retrofit.create(SuggestionApi::class.java)
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class SuggestionRepositoryModule {
    @Binds
    internal abstract fun bindSuggestionRepository(impl: SuggestionRepositoryImpl): SuggestionRepository
}
