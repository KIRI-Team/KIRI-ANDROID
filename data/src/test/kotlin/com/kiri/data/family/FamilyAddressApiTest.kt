package com.kiri.data.family
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.family.FamilyAddress
import kotlinx.coroutines.test.runTest
import mockwebserver3.*
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import org.junit.Assert.*
import org.junit.Test
class FamilyAddressApiTest {
    @Test fun restoresIdAndSavesCoordinatesOnlyAfterValidation() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"OK","message":"ok","result":{"familyId":7}}""").build())
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"OK","message":"ok","result":null}""").build())
            val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType())).build().create(FamilyApi::class.java)
            val ids = FakeFamilyIdStore(); val repo = FamilyRepositoryImpl(api, ids, kiriJson)
            val address = FamilyAddress("06134","서울 서초구 강남대로 143","810호","1165010800",37.5,127.0)
            assertTrue(repo.saveAddress(address.copy(latitude = Double.NaN)).isFailure); assertEquals(0,server.requestCount)
            repo.saveAddress(address).getOrThrow(); assertEquals(7L,ids.saved)
            assertEquals("/api/v1/families/me/id",server.takeRequest().url.encodedPath)
            val request=server.takeRequest(); assertEquals("PUT",request.method); assertEquals("/api/v1/families/7/address",request.url.encodedPath)
            assertTrue(request.body!!.utf8().contains("\"regionCode\":\"1165010800\""))
        }
    }
}
