package com.kiri.data.family.remote
import com.kiri.data.common.network.kiriJson
import com.kiri.domain.family.model.FamilyAddress
import kotlinx.coroutines.test.runTest
import mockwebserver3.*
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import org.junit.Assert.*
import org.junit.Test
import com.kiri.data.family.local.FakeFamilyIdStore
import com.kiri.data.family.repository.FamilyRepositoryImpl
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

    @Test fun loadsSavedAddressAndTreatsNullFieldsAsNotRegistered() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"OK","message":"ok","result":{"familyId":7}}""").build())
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"FAMILY200_10","message":"ok","result":{"familyId":7,"postalCode":"06134","address":"서울특별시 서초구 강남대로 143","detailAddress":"810동 603호","regionCode":"1165010800","latitude":37.5519,"longitude":126.9254}}""").build())
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"FAMILY200_10","message":"ok","result":{"familyId":7,"postalCode":null,"address":null,"detailAddress":null,"regionCode":null,"latitude":null,"longitude":null}}""").build())
            val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType())).build().create(FamilyApi::class.java)
            val repo = FamilyRepositoryImpl(api, FakeFamilyIdStore(), kiriJson)

            assertEquals(
                FamilyAddress("06134", "서울특별시 서초구 강남대로 143", "810동 603호", "1165010800", 37.5519, 126.9254),
                repo.getAddress().getOrThrow(),
            )
            assertEquals("/api/v1/families/me/id", server.takeRequest().url.encodedPath)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/families/7/address", request.url.encodedPath)

            // 주소를 등록하지 않은 가족은 주소 필드가 null 로 온다
            assertNull(repo.getAddress().getOrThrow())
        }
    }

    @Test fun failsWhenNotFamilyMember() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"OK","message":"ok","result":{"familyId":7}}""").build())
            server.enqueue(MockResponse.Builder().code(403).body("""{"isSuccess":false,"code":"FAMILY403_1","message":"해당 가족에 대한 권한이 없습니다.","result":null}""").build())
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"OK","message":"ok","result":{"familyId":7}}""").build())
            val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType())).build().create(FamilyApi::class.java)
            val repo = FamilyRepositoryImpl(api, FakeFamilyIdStore(), kiriJson)

            assertTrue(repo.getAddress().isFailure)
        }
    }
}
