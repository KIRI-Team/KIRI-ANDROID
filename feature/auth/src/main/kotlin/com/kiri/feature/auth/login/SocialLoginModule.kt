package com.kiri.feature.auth.login

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// 화면에서는 SocialLogin 만 쓰고, 실제 SDK 구현은 여기서 연결한다 (UI 테스트에서는 @TestInstallIn 으로 교체)
@Module
@InstallIn(SingletonComponent::class)
abstract class SocialLoginModule {
    @Binds
    abstract fun bindSocialLogin(impl: SdkSocialLogin): SocialLogin
}
