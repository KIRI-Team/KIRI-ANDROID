package com.kiri.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// 초대 토큰이 기기 저장소에 남아, 앱을 다시 켜도(로그인·온보딩 도중 종료) 가족 참여 전까지 유지되는지 확인한다
class DataStorePendingInviteRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    private val file by lazy { File(folder.root, "pending_invite.preferences_pb") }

    // 같은 파일을 여는 새 저장소 = 앱을 다시 켠 상황. 같은 파일을 동시에 여는 DataStore 는 허용되지 않아 이전 것을 닫는다
    private suspend fun reopen(): DataStorePendingInviteRepository {
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return DataStorePendingInviteRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @Test
    fun 저장한_토큰은_꺼내도_지워지지_않고_앱을_다시_켜도_남아_있다() = runTest {
        val repository = reopen()
        repository.save("eyJhbGciOiJIUzI1NiJ9.payload.sig")

        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", repository.get())
        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", repository.get())
        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", reopen().get())
    }

    @Test
    fun 새_링크의_토큰이_이전_토큰을_덮어쓴다() = runTest {
        val repository = reopen()
        repository.save("old-token")
        repository.save("new-token")

        assertEquals("new-token", repository.get())
    }

    @Test
    fun 지우면_다시_켜도_토큰이_없고_빈_토큰은_저장하지_않는다() = runTest {
        val repository = reopen()
        assertNull(repository.get())

        repository.save("token")
        repository.clear()
        assertNull(reopen().get())

        val reopened = reopen()
        reopened.save(" ")
        assertNull(reopened.get())
    }
}
