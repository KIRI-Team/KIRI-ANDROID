package com.kiri.data.device

import com.kiri.data.auth.AuthTokens
import com.kiri.data.auth.FakeTokenStore
import com.kiri.data.device.PushDeviceSync.Companion.FIRST_RETRY_DELAY_MS
import com.kiri.data.device.PushDeviceSync.Companion.MAX_ATTEMPTS
import com.kiri.data.network.KiriApiException
import com.kiri.domain.repository.PushDeviceRepository
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// 로그인한 동안에만 이 기기의 FCM 토큰을 서버에 등록하고, 연결·서버 문제로 실패하면 다시 시도한다
@OptIn(ExperimentalCoroutinesApi::class)
class PushDeviceSyncTest {

    private val tokens = FakeTokenStore()
    private val devices = RecordingDevices()

    private fun sync(pushToken: PushTokenProvider = PushTokenProvider { "fcm-1" }) =
        PushDeviceSync(tokens, pushToken, devices)

    @Test
    fun 로그인하면_등록하고_토큰_재발급에는_다시_등록하지_않는다() = runTest {
        backgroundScope.launch { sync().run() }
        runCurrent()
        assertEquals(emptyList<String>(), devices.registered)

        tokens.save(AuthTokens("access-1", "refresh-1"))
        runCurrent()
        assertEquals(listOf("fcm-1"), devices.registered)

        // 액세스 토큰만 바뀐 것(재발급)은 로그인 상태가 그대로다
        tokens.save(AuthTokens("access-2", "refresh-2"))
        runCurrent()
        assertEquals(listOf("fcm-1"), devices.registered)

        // 로그아웃 뒤 다시 로그인하면 또 등록한다
        tokens.clear()
        runCurrent()
        tokens.save(AuthTokens("access-3", "refresh-3"))
        runCurrent()
        assertEquals(listOf("fcm-1", "fcm-1"), devices.registered)
    }

    @Test
    fun 앱을_켰을_때_이미_로그인되어_있으면_바로_등록한다() = runTest {
        tokens.save(AuthTokens("access-1", "refresh-1"))

        backgroundScope.launch { sync().run() }
        runCurrent()

        assertEquals(listOf("fcm-1"), devices.registered)
    }

    @Test
    fun 연결이_끊겨_실패하면_10초부터_간격을_두_배씩_늘려_다시_시도한다() = runTest {
        devices.results += listOf(Result.failure(IOException()), Result.failure(IOException()), Result.success(Unit))
        tokens.save(AuthTokens("access-1", "refresh-1"))

        backgroundScope.launch { sync().run() }
        runCurrent()
        assertEquals(1, devices.registered.size)

        advanceTimeBy(FIRST_RETRY_DELAY_MS - 1)
        runCurrent()
        assertEquals(1, devices.registered.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, devices.registered.size)

        advanceTimeBy(FIRST_RETRY_DELAY_MS * 2)
        runCurrent()
        assertEquals(3, devices.registered.size)

        // 성공한 뒤에는 더 보내지 않는다
        advanceUntilIdle()
        assertEquals(3, devices.registered.size)
    }

    @Test
    fun 서버_오류면_다시_시도하고_요청_값_오류면_멈춘다() = runTest {
        devices.results += listOf(
            Result.failure(KiriApiException("COMMON500_1", "서버 내부 오류가 발생했습니다.")),
            Result.failure(KiriApiException("HTTP502", "Bad Gateway")),
            Result.failure(KiriApiException("COMMON400_1", "잘못된 요청입니다.")),
        )
        tokens.save(AuthTokens("access-1", "refresh-1"))

        backgroundScope.launch { sync().run() }
        advanceUntilIdle()

        assertEquals(3, devices.registered.size)
    }

    @Test
    fun 계속_실패하면_정해진_횟수까지만_시도한다() = runTest {
        devices.results += List(MAX_ATTEMPTS + 3) { Result.failure(IOException()) }
        tokens.save(AuthTokens("access-1", "refresh-1"))

        backgroundScope.launch { sync().run() }
        advanceUntilIdle()

        assertEquals(MAX_ATTEMPTS, devices.registered.size)
    }

    @Test
    fun 다시_시도를_기다리는_중에_로그아웃하면_멈춘다() = runTest {
        devices.results += List(MAX_ATTEMPTS) { Result.failure(IOException()) }
        tokens.save(AuthTokens("access-1", "refresh-1"))

        backgroundScope.launch { sync().run() }
        runCurrent()
        tokens.clear()
        advanceUntilIdle()

        assertEquals(1, devices.registered.size)
    }

    @Test
    fun FCM_토큰을_받지_못하면_다시_시도할_때_새로_받는다() = runTest {
        var attempt = 0
        val flaky = PushTokenProvider {
            when (attempt++) {
                0 -> error("Firebase 오류")
                1 -> null
                else -> "fcm-2"
            }
        }
        tokens.save(AuthTokens("access-1", "refresh-1"))

        backgroundScope.launch { sync(flaky).run() }
        advanceUntilIdle()

        assertEquals(3, attempt)
        assertEquals(listOf("fcm-2"), devices.registered)
    }

    @Test
    fun 새_토큰은_로그인했을_때만_등록한다() = runTest {
        val sync = sync()

        sync.onNewToken("fcm-new")
        assertEquals(emptyList<String>(), devices.registered)

        tokens.save(AuthTokens("access-1", "refresh-1"))
        sync.onNewToken("fcm-new")
        assertEquals(listOf("fcm-new"), devices.registered)
    }

    @Test
    fun 새_토큰_등록이_실패해도_다시_시도한다() = runTest {
        devices.results += listOf(Result.failure(IOException()), Result.success(Unit))
        tokens.save(AuthTokens("access-1", "refresh-1"))

        sync().onNewToken("fcm-new")

        assertEquals(listOf("fcm-new", "fcm-new"), devices.registered)
    }

    // results 에 넣은 순서대로 돌려주고, 다 쓰면 성공한다
    private class RecordingDevices : PushDeviceRepository {
        val registered = mutableListOf<String>()
        val results = ArrayDeque<Result<Unit>>()
        override suspend fun register(fcmToken: String): Result<Unit> {
            registered += fcmToken
            return results.removeFirstOrNull() ?: Result.success(Unit)
        }
        override suspend fun unregister() = Result.success(Unit)
    }
}
