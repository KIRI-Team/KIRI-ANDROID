package com.kiri.app.record

import com.kiri.data.condition.ScheduleConditionRepositoryModule
import com.kiri.domain.model.condition.ConditionCustom
import com.kiri.domain.repository.ScheduleConditionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 실제 서버 대신 적용한 조건이 없는 것으로 보고, 적용은 성공한 것으로 본다
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ScheduleConditionRepositoryModule::class])
object FakeScheduleConditionModule {
    @Provides
    fun provideScheduleConditionRepository(): ScheduleConditionRepository = object : ScheduleConditionRepository {
        override suspend fun getConditionCustom(scheduleId: Long): Result<ConditionCustom> =
            Result.success(ConditionCustom("", null, null, null, emptySet()))

        override suspend fun applyConditions(scheduleId: Long, conditions: Set<com.kiri.domain.detail.Condition>): Result<Unit> =
            Result.success(Unit)
    }
}
