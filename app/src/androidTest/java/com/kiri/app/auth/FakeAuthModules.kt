package com.kiri.app.auth

import android.app.Activity
import com.kiri.data.auth.AuthRepositoryModule
import com.kiri.data.term.TermRepositoryModule
import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.model.term.Term
import com.kiri.domain.repository.AuthRepository
import com.kiri.domain.repository.TermRepository
import com.kiri.feature.auth.login.SocialLogin
import com.kiri.feature.auth.login.SocialLoginModule
import com.kiri.feature.auth.login.SocialLoginResult
import com.kiri.feature.auth.model.LoginProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// UI 테스트에서는 카카오·구글 창을 띄우지 않고 바로 로그인에 성공한다
class FakeSocialLogin : SocialLogin {
    override suspend fun login(activity: Activity, provider: LoginProvider, nonce: String): SocialLoginResult =
        SocialLoginResult.Success(provider, "test-id-token")
}

// UI 테스트에서는 실제 서버 대신 새 회원으로 로그인한 것으로 본다 (로그인 → 약관동의 → 온보딩)
class FakeAuthRepository(
    private val result: LoginResult = LoginResult(isNewUser = true, termsAgreed = false),
) : AuthRepository {
    // 실제 저장소처럼 로그인에 성공해야 로그인 상태가 된다
    private val loggedIn = MutableStateFlow(false)
    override val isLoggedIn: Flow<Boolean> = loggedIn

    override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> {
        loggedIn.value = true
        return Result.success(result)
    }

    // 토큰 재발급이 거절된 상황을 흉내 낸다
    fun expireSession() {
        loggedIn.value = false
    }
}

// UI 테스트에서는 실제 서버 대신 고정된 약관 목록을 주고, 동의는 바로 저장된 것으로 본다
class FakeTermRepository(
    private val terms: List<Term> = FAKE_TERMS,
) : TermRepository {
    override suspend fun getTerms(): Result<List<Term>> = Result.success(terms)

    override suspend fun agree(agreements: Map<Long, Boolean>): Result<Unit> = Result.success(Unit)
}

val FAKE_TERMS = listOf(
    Term(id = 1, title = "서비스 이용약관", content = "제1조 (목적)\n서비스 이용 조건을 정합니다.", required = true),
    Term(id = 2, title = "개인정보 처리방침", content = "제1조 (수집 항목)\n수집하는 정보를 안내합니다.", required = true),
    Term(id = 3, title = "마케팅 정보 수신 동의", content = "이벤트, 혜택 안내", required = false),
)

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SocialLoginModule::class])
object FakeSocialLoginModule {
    @Provides
    fun provideSocialLogin(): SocialLogin = FakeSocialLogin()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AuthRepositoryModule::class])
object FakeAuthRepositoryModule {
    // 화면(AuthViewModel)과 MainActivity 가 같은 로그인 상태를 보도록 하나만 만든다
    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = FakeAuthRepository()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [TermRepositoryModule::class])
object FakeTermRepositoryModule {
    @Provides
    fun provideTermRepository(): TermRepository = FakeTermRepository()
}
