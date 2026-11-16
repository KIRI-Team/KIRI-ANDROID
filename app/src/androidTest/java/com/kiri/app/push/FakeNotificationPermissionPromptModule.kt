package com.kiri.app.push

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 알림 권한 창을 띄우지 않는다. 시스템 창이 화면을 가려 다음 동작을 막는다
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NotificationPermissionPromptModule::class])
object FakeNotificationPermissionPromptModule {
    @Provides
    @NotificationPermissionPromptEnabled
    fun provideNotificationPermissionPromptEnabled(): Boolean = false
}
