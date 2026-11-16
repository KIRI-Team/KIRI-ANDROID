package com.kiri.data.device

import com.kiri.data.auth.AuthTokens
import com.kiri.data.auth.FakeTokenStore
import com.kiri.domain.repository.PushDeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// 로그인한 동안에만 이 기기의 FCM 토큰을 서버에 등록한다
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
    fun FCM_토큰을_받지_못하면_등록하지_않고_계속_돈다() = runTest {
        var attempt = 0
        val flaky = PushTokenProvider { if (attempt++ == 0) error("Firebase 오류") else null }
        tokens.save(AuthTokens("access-1", "refresh-1"))

        backgroundScope.launch { sync(flaky).run() }
        runCurrent()
        tokens.clear()
        runCurrent()
        tokens.save(AuthTokens("access-2", "refresh-2"))
        runCurrent()

        assertEquals(2, attempt)
        assertEquals(emptyList<String>(), devices.registered)
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

    private class RecordingDevices : PushDeviceRepository {
        val registered = mutableListOf<String>()
        override suspend fun register(fcmToken: String): Result<Unit> {
            registered += fcmToken
            return Result.success(Unit)
        }
        override suspend fun unregister() = Result.success(Unit)
    }
}
