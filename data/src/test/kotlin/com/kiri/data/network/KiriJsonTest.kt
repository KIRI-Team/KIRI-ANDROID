package com.kiri.data.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class KiriJsonTest {

    @Serializable
    private data class SampleResponse(val id: Long, val nickname: String, val familyName: String? = null)

    private interface SampleApi {
        @GET("sample")
        suspend fun get(): SampleResponse
    }

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun api(): SampleApi = Retrofit.Builder()
        .baseUrl(apiBaseUrl(server.url("/").toString()))
        .client(OkHttpClient())
        .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SampleApi::class.java)

    @Test
    fun 서버가_모르는_필드를_추가해도_읽고_없는_선택_필드는_기본값으로_둔다() = runTest {
        server.enqueue(MockResponse.Builder().body("""{"id":1,"nickname":"끼리","newField":true}""").build())

        val response = api().get()

        assertEquals(SampleResponse(id = 1, nickname = "끼리"), response)
        assertNull(response.familyName)
        assertEquals("/sample", server.takeRequest().url.encodedPath)
    }
}
