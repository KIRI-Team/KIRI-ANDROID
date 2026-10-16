package com.kiri.feature.auth.login

import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// 화면에서는 SocialLogin 만 쓰고, 실제 SDK 구현은 여기서 연결한다 (UI 테스트에서는 @TestInstallIn 으로 교체)
@Module
@InstallIn(SingletonComponent::class)
abstract class SocialLoginModule {
    @Binds
    abstract fun bindSocialLogin(impl: SdkSocialLogin): SocialLogin
}

// SDK 로그인은 Activity 가 필요해 ViewModel 이 아닌 화면(AuthRoute)에서 실행한다. 화면에서 꺼내 쓰는 통로
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SocialLoginEntryPoint {
    fun socialLogin(): SocialLogin
}
