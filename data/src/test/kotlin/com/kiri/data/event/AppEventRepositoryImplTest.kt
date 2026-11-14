package com.kiri.data.event

import com.kiri.data.network.kiriJson
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppEventRepositoryImplTest {
    @Test
    fun sendsMissionDetailViewWithKoreanLocalTime() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse.Builder().body(
                    """{"isSuccess":true,"code":"COMMON200","message":"OK","result":{"inserted":1}}""",
                ).build(),
            )
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AppEventApi::class.java)
            val clock = Clock.fixed(Instant.parse("2026-11-12T14:05:00Z"), ZoneOffset.UTC)
            val repository = AppEventRepositoryImpl(api, kiriJson, clock)

            assertTrue(repository.recordMissionDetailView(42).isSuccess)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/events", request.url.encodedPath)
            val event = kiriJson.decodeFromString<AppEventBatch>(request.body!!.utf8()).events.single()
            assertEquals(event.eventId, UUID.fromString(event.eventId).toString())
            assertEquals("mission_detail_view", event.eventName)
            assertEquals(mapOf("missionId" to 42L), event.properties)
            assertEquals("ANDROID", event.platform)
            assertEquals("2026-11-12T23:05:00", event.occurredAt)
        }
    }
}
