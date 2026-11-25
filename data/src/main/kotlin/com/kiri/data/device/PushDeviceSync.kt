package com.kiri.data.device

import com.kiri.data.auth.TokenStore
import com.kiri.data.network.KiriApiException
import com.kiri.domain.repository.PushDeviceRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
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
class PushDeviceSync @Inject internal constructor(
    private val tokens: TokenStore,
    private val pushTokens: PushTokenProvider,
    private val devices: PushDeviceRepository,
) {
    // 로그인 상태가 될 때마다 등록한다. 앱을 켰을 때 이미 로그인되어 있어도 한 번 등록해 바뀐 토큰을 맞춘다.
    // 로그인 토큰 재발급은 로그인 상태가 그대로라 다시 등록하지 않는다. 앱이 살아 있는 동안 계속 돈다.
    // 다시 시도하는 중에 로그아웃하면 시도를 멈춘다 (collectLatest 가 이전 작업을 취소한다)
    suspend fun run() {
        tokens.tokens.map { it != null }.distinctUntilChanged().collectLatest { loggedIn ->
            if (loggedIn) registerWithRetry(newToken = null)
        }
    }

    // Firebase 가 새 토큰을 줬을 때. 로그인하지 않았으면 다음 로그인 때 run() 이 등록한다
    suspend fun onNewToken(token: String) {
        registerWithRetry(newToken = token)
    }

    // 연결 문제·서버 오류(5xx)·FCM 토큰을 받지 못한 경우에만 간격을 두 배씩 늘리며 다시 시도한다.
    // 요청 값 오류(4xx)는 다시 보내도 같아서 멈춘다. 끝내 실패하면 다음 실행·로그인·토큰 변경 때 다시 등록된다.
    // newToken 이 없으면 시도할 때마다 Firebase 에서 현재 토큰을 다시 받아, 그 사이 토큰이 바뀌어도 옛 토큰을 보내지 않는다
    private suspend fun registerWithRetry(newToken: String?) {
        var wait = FIRST_RETRY_DELAY_MS
        for (attempt in 1..MAX_ATTEMPTS) {
            if (tokens.get() == null) return
            val token = newToken ?: pushTokens.tokenOrNull()
            val result = token?.let { devices.register(it) }
            if (result != null && (result.isSuccess || !result.exceptionOrNull().isRetryable())) return
            if (attempt == MAX_ATTEMPTS) return
            delay(wait)
            wait = (wait * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        }
    }

    private fun Throwable?.isRetryable(): Boolean = when (this) {
        is IOException -> true
        is KiriApiException -> code.startsWith("HTTP5") || code.startsWith("COMMON500")
        else -> false
    }

    private suspend fun PushTokenProvider.tokenOrNull(): String? = try {
        token()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    internal companion object {
        // 10초, 20초, 40초, 80초, 160초 뒤 다시 시도한다 (모두 6번, 약 5분)
        const val MAX_ATTEMPTS = 6
        const val FIRST_RETRY_DELAY_MS = 10_000L
        const val MAX_RETRY_DELAY_MS = 5 * 60_000L
    }
}
