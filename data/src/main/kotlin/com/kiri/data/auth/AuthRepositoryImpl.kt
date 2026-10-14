package com.kiri.data.auth

import com.kiri.data.network.apiResult
import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : AuthRepository {

    override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> =
        apiResult(json) {
            api.login(LoginRequest(socialProvider = provider.name, socialProviderToken = socialToken, nonce = nonce))
        }.mapCatching { response ->
            tokenStore.save(AuthTokens(response.accessToken, response.refreshToken))
            LoginResult(isNewUser = response.isNewUser, termsAgreed = response.termsAgreed)
        }
}
