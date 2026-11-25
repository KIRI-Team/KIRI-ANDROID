package com.kiri.data.auth

import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.auth.model.LoginResult
import com.kiri.domain.auth.model.SocialProvider
import com.kiri.domain.auth.repository.AuthRepository
import com.kiri.domain.push.repository.PushDeviceRepository
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
    private val devices: PushDeviceRepository,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = tokenStore.tokens.map { it != null }.distinctUntilChanged()

    override suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult> =
        apiResult(json) {
            api.login(LoginRequest(socialProvider = provider.name, socialProviderToken = socialToken, nonce = nonce))
        }.mapCatching { response ->
            tokenStore.clear()
            tokenStore.save(AuthTokens(response.accessToken, response.refreshToken))
            LoginResult(isNewUser = response.isNewUser, termsAgreed = response.termsAgreed)
        }

    // 요청이 취소되더라도 토큰은 지워야 로그아웃한 사용자가 로그인 상태로 남지 않는다.
    // 기기 등록은 로그인 토큰이 있어야 지울 수 있어 먼저 지운다. 실패해도(등록 안 된 기기 등) 로그아웃은 그대로 한다
    override suspend fun logout(): Result<Unit> = try {
        devices.unregister()
        apiCommand(json) { api.logout() }
    } finally {
        withContext(NonCancellable) { tokenStore.clear() }
    }

    // 탈퇴한 계정으로 이 기기에 알림이 오지 않도록 기기 등록을 먼저 지운다.
    // 탈퇴가 실패해 로그인 상태로 남으면 다음에 앱을 켤 때 PushDeviceSync 가 다시 등록한다
    override suspend fun withdraw(): Result<Unit> {
        devices.unregister()
        return apiCommand(json) { api.withdraw() }.onSuccess { tokenStore.clear() }
    }
}
