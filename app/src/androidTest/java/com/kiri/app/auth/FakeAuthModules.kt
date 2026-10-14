package com.kiri.app.auth

import android.app.Activity
import com.kiri.data.auth.AuthRepositoryModule
import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.repository.AuthRepository
import com.kiri.feature.auth.login.SocialLogin
import com.kiri.feature.auth.login.SocialLoginModule
import com.kiri.feature.auth.login.SocialLoginResult
import com.kiri.feature.auth.model.LoginProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 카카오·구글 창을 띄우지 않고 바로 로그인에 성공한다
class FakeSocialLogin : SocialLogin {
    override suspend fun login(activity: Activity, provider: LoginProvider, nonce: String): SocialLoginResult =
        SocialLoginResult.Success(provider, "test-id-token")
}

// UI 테스트에서는 실제 서버 대신 새 회원으로 로그인한 것으로 본다 (로그인 → 약관동의 → 온보딩)
class FakeAuthRepository(
    private val result: LoginResult = LoginResult(isNewUser = true, termsAgreed = false),
) : AuthRepository {
    override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> =
        Result.success(result)
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SocialLoginModule::class])
object FakeSocialLoginModule {
    @Provides
    fun provideSocialLogin(): SocialLogin = FakeSocialLogin()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AuthRepositoryModule::class])
object FakeAuthRepositoryModule {
    @Provides
    fun provideAuthRepository(): AuthRepository = FakeAuthRepository()
}
