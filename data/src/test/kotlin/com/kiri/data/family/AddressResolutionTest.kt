package com.kiri.data.family

import com.kiri.data.network.kiriJson
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AddressResolutionTest {
    private fun repository(geocoder: AddressGeocoder) = FamilyRepositoryImpl(
        Retrofit.Builder().baseUrl("https://example.invalid/")
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build().create(FamilyApi::class.java), FakeFamilyIdStore(), kiriJson, geocoder)

    @Test fun postcodeMetadataAndCoordinatesAreKeptTogether() = runTest {
        var query = ""
        val repo = repository { query = it; AddressCoordinates(37.5, 127.0) }
        val result = repo.resolveAddress("06134", " 서울 서초구 강남대로 143 ", "1165010800").getOrThrow()
        assertEquals("서울 서초구 강남대로 143", query)
        assertEquals(query, result.address)
        assertEquals("06134", result.postalCode)
        assertEquals("1165010800", result.regionCode)
        assertEquals(37.5, result.latitude, 0.0)
        assertEquals(127.0, result.longitude, 0.0)
    }
    @Test fun invalidSelectionNeverCallsGeocoder() = runTest {
        var calls = 0
        val repo = repository { calls++; AddressCoordinates(37.5, 127.0) }
        assertTrue(repo.resolveAddress("123", "주소", "1165010800").isFailure)
        assertTrue(repo.resolveAddress("06134", "주소", "").isFailure)
        assertTrue(repo.resolveAddress("06134", " ", "1165010800").isFailure)
        assertEquals(0, calls)
    }
    @Test fun missingAndInvalidCoordinatesNeverBecomeSavableAddresses() = runTest {
        assertTrue(repository { null }.resolveAddress("06134", "주소", "1165010800").isFailure)
        assertTrue(repository { AddressCoordinates(Double.NaN, 127.0) }.resolveAddress("06134", "주소", "1165010800").isFailure)
        assertTrue(repository { throw IOException("위치 확인 실패") }.resolveAddress("06134", "주소", "1165010800").isFailure)
    }
    @Test fun unresponsiveProviderTimesOutAndParentCancellationIsNotSwallowed() = runTest {
        val repo = repository { awaitCancellation() }
        assertTrue(repo.resolveAddress("06134", "주소", "1165010800").isFailure)
        var returned = false
        val job = launch { repo.resolveAddress("06134", "주소", "1165010800"); returned = true }
        runCurrent(); job.cancel(); job.join()
        assertFalse(returned)
    }
}
