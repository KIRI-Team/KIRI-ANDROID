package com.kiri.data.auth

import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = tokenStore.tokens.map { it != null }.distinctUntilChanged()

    override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> =
        apiResult(json) {
            api.login(LoginRequest(socialProvider = provider.name, socialProviderToken = socialToken, nonce = nonce))
        }.mapCatching { response ->
            tokenStore.save(AuthTokens(response.accessToken, response.refreshToken))
            LoginResult(isNewUser = response.isNewUser, termsAgreed = response.termsAgreed)
        }

    // 요청이 취소되더라도 토큰은 지워야 로그아웃한 사용자가 로그인 상태로 남지 않는다
    override suspend fun logout(): Result<Unit> = try {
        apiCommand(json) { api.logout() }
    } finally {
        withContext(NonCancellable) { tokenStore.clear() }
    }
}
