package com.kiri.app.push

import com.google.firebase.messaging.FirebaseMessaging
import com.kiri.data.push.PushTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.suspendCancellableCoroutine

// 앱이 살아 있는 동안 도는 기기 등록 작업용. 서비스·화면이 끝나도 요청이 끊기지 않게 한다
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PushScope

@Module
@InstallIn(SingletonComponent::class)
object PushModule {
    @Provides
    @Singleton
    fun providePushTokenProvider(): PushTokenProvider = FirebasePushTokenProvider

    @Provides
    @Singleton
    @PushScope
    fun providePushScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

// 알림 권한 창을 띄울지. UI 테스트에서는 시스템 권한 창이 화면을 가리지 않도록 false 로 바꾼다
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NotificationPermissionPromptEnabled

@Module
@InstallIn(SingletonComponent::class)
object NotificationPermissionPromptModule {
    @Provides
    @NotificationPermissionPromptEnabled
    fun provideNotificationPermissionPromptEnabled(): Boolean = true
}

// Firebase 에서 이 기기의 FCM 토큰을 받는다. 실패하거나 취소되면 null
private object FirebasePushTokenProvider : PushTokenProvider {
    override suspend fun token(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
            .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
            .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
    }
}
