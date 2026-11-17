package com.kiri.data.notification

import com.kiri.data.network.kiriJson
import com.kiri.domain.notification.NotificationCategory
import com.kiri.domain.notification.NotificationTarget
import java.time.Instant
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteNotificationRepositoryTest {
    @Test
    fun loadsAllPagesAndAcceptsBothResponseVersions() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(page(false, invitation))
            server.enqueue(page(true, """{"notificationId":2,"title":"완료","body":"기록","notificationType":"RECORD_CREATED","category":"RECORD","targetType":"SCHEDULE_RECORD","isRead":true,"referenceId":19,"createdAt":"2026-11-14T12:00:00Z"}"""))
            server.enqueue(success("null"))
            val repository = repository(server)

            val (first, second) = repository.load()
            assertEquals(NotificationTarget.SCHEDULE_INVITATION, first.target)
            assertEquals(NotificationCategory.SCHEDULE, first.category)
            assertEquals("10", first.targetId)
            assertEquals(Instant.parse("2026-11-15T12:00:00Z"), first.createdAt)
            assertFalse(first.read)
            assertEquals(NotificationTarget.MEMORY, second.target)
            assertTrue(second.read)
            repository.markRead(first.id)

            assertEquals("page=0&size=20", server.takeRequest().url.encodedQuery)
            assertEquals("page=1&size=20", server.takeRequest().url.encodedQuery)
            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/notifications/1/read", request.url.encodedPath)
        }
    }

    @Test
    fun usesIndividualRequestsOnlyWhenBulkEndpointIsMissing() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(success("""{"readCount":2}"""))
            server.enqueue(MockResponse.Builder().code(405).build())
            server.enqueue(page(true, invitation))
            server.enqueue(success("null"))
            val repository = repository(server)

            repository.markAllRead()
            repository.markAllRead()

            assertEquals("/api/v1/notifications/read-all", server.takeRequest().url.encodedPath)
            assertEquals("/api/v1/notifications/read-all", server.takeRequest().url.encodedPath)
            assertEquals("GET", server.takeRequest().method)
            assertEquals("/api/v1/notifications/1/read", server.takeRequest().url.encodedPath)
        }
    }

    private fun repository(server: MockWebServer): RemoteNotificationRepository {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
        return RemoteNotificationRepository(retrofit.create(NotificationApi::class.java))
    }

    private fun page(last: Boolean, item: String) = success("""{"last":$last,"content":[$item]}""")

    private fun success(result: String) = MockResponse.Builder()
        .body("""{"isSuccess":true,"code":"OK","message":"ok","result":$result}""")
        .build()

    private val invitation = """{"notificationId":1,"title":"초대","body":"새 일정","notificationType":"SCHEDULE_INVITATION","isRead":false,"referenceId":10,"createdAt":"2026-11-15T21:00:00"}"""
}
