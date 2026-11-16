package com.kiri.data.device

import com.kiri.data.auth.TokenStore
import com.kiri.domain.repository.PushDeviceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// 이 기기의 FCM 토큰. Firebase 는 앱 모듈에만 있어서 앱이 구현한다. 받지 못하면 null
fun interface PushTokenProvider {
    suspend fun token(): String?
}

// 로그인한 동안 이 기기의 FCM 토큰이 서버에 등록되어 있도록 맞춘다.
// 등록을 지우는 일은 로그아웃·탈퇴 때 AuthRepositoryImpl 이 한다 (로그인 토큰이 남아 있을 때 지워야 해서)
@Singleton
class PushDeviceSync @Inject constructor(
    private val tokens: TokenStore,
    private val pushTokens: PushTokenProvider,
    private val devices: PushDeviceRepository,
) {
    // 로그인 상태가 될 때마다 등록한다. 앱을 켰을 때 이미 로그인되어 있어도 한 번 등록해 바뀐 토큰을 맞춘다.
    // 로그인 토큰 재발급은 로그인 상태가 그대로라 다시 등록하지 않는다. 앱이 살아 있는 동안 계속 돈다
    suspend fun run() {
        tokens.tokens.map { it != null }.distinctUntilChanged().collectLatest { loggedIn ->
            if (loggedIn) pushTokens.tokenOrNull()?.let { devices.register(it) }
        }
    }

    // Firebase 가 새 토큰을 줬을 때. 로그인하지 않았으면 다음 로그인 때 run() 이 등록한다
    suspend fun onNewToken(token: String) {
        if (tokens.get() != null) devices.register(token)
    }

    private suspend fun PushTokenProvider.tokenOrNull(): String? = try {
        token()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
