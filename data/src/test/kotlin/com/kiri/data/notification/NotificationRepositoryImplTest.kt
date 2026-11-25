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

class NotificationRepositoryImplTest {
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

    // 요청알림(SCHEDULE_INVITATION)과 일정 변경알림(SCHEDULE_CHANGED·재확인 알림)은 응답 화면으로, 그 밖의 일정 알림은 상세로 연다
    @Test
    fun scheduleChangeNotificationsOpenTheResponseScreen() = runTest {
        MockWebServer().use { server ->
            server.start()
            val items = listOf(
                notification(1, "SCHEDULE_CHANGED", "SCHEDULE"),
                notification(2, "SCHEDULE_RECONFIRM_REMINDER", null),
                notification(3, "SCHEDULE_CONFIRMED_UNKNOWN", "SCHEDULE"),
                notification(4, "SCHEDULE_D1_REMINDER", null),
            ).joinToString(",")
            server.enqueue(page(true, items))
            val targets = repository(server).load().map { it.target }

            assertEquals(
                listOf(
                    NotificationTarget.SCHEDULE_INVITATION,
                    NotificationTarget.SCHEDULE_INVITATION,
                    NotificationTarget.SCHEDULE,
                    NotificationTarget.SCHEDULE,
                ),
                targets,
            )
        }
    }

    private fun notification(id: Int, type: String, targetType: String?) =
        "{\"notificationId\":$id,\"title\":\"일정\",\"body\":\"알림\",\"notificationType\":\"$type\"," +
            (targetType?.let { "\"targetType\":\"$it\"," } ?: "") +
            "\"isRead\":false,\"referenceId\":10,\"createdAt\":\"2026-11-15T21:00:00\"}"

    private fun repository(server: MockWebServer): NotificationRepositoryImpl {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
        return NotificationRepositoryImpl(retrofit.create(NotificationApi::class.java))
    }

    private fun page(last: Boolean, item: String) = success("""{"last":$last,"content":[$item]}""")

    private fun success(result: String) = MockResponse.Builder()
        .body("""{"isSuccess":true,"code":"OK","message":"ok","result":$result}""")
        .build()

    private val invitation = """{"notificationId":1,"title":"초대","body":"새 일정","notificationType":"SCHEDULE_INVITATION","isRead":false,"referenceId":10,"createdAt":"2026-11-15T21:00:00"}"""
}
