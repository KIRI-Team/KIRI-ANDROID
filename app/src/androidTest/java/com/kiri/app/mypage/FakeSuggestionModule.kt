package com.kiri.app.mypage

import com.kiri.data.suggestion.SuggestionRepositoryModule
import com.kiri.domain.repository.SuggestionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 실제 서버 대신 건의가 바로 보내진 것으로 본다
class FakeSuggestionRepository : SuggestionRepository {
    override suspend fun send(content: String, imageKey: String?): Result<Long> = Result.success(1L)
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SuggestionRepositoryModule::class])
object FakeSuggestionRepositoryModule {
    @Provides
    fun provideSuggestionRepository(): SuggestionRepository = FakeSuggestionRepository()
}
