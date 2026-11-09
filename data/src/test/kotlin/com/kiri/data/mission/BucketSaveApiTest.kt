package com.kiri.data.mission
import com.kiri.data.network.kiriJson
import kotlinx.coroutines.test.runTest
import mockwebserver3.*
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import org.junit.Assert.*
import org.junit.Test
class BucketSaveApiTest {
    @Test fun savesViaMissionEndpointAndPreservesServerFailure() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"OK","message":"ok","result":null}""").build())
            server.enqueue(MockResponse.Builder().code(409).body("""{"isSuccess":false,"code":"BUCKET409_1","message":"already saved","result":null}""").build())
            val api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType())).build().create(MissionApi::class.java)
            val repo=MissionInfoRepositoryImpl(api,kiriJson); repo.saveBucket(55).getOrThrow()
            val request=server.takeRequest(); assertEquals("POST",request.method); assertEquals("/api/v1/missions/55/bucket",request.url.encodedPath)
            assertTrue(repo.saveBucket(55).isFailure)
        }
    }
}
