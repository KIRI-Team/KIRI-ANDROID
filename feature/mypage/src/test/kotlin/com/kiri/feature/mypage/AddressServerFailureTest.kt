package com.kiri.feature.mypage

import com.kiri.domain.repository.FamilyRepository
import com.kiri.domain.model.family.*
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddressServerFailureTest {
    @get:Rule val main = MainDispatcherRule()
    private class FamilyFake : FamilyRepository {
        var resolveFails = false
        var saveFails = true
        var saved: FamilyAddress? = null
        override suspend fun createFamily(name: String): Result<com.kiri.domain.model.family.Family> = error("unused")
        override suspend fun renameFamily(name: String) = Result.success(name)
        override suspend fun resolveAddress(postalCode: String, address: String, regionCode: String): Result<FamilyAddress> =
            if (resolveFails) Result.failure(Exception("좌표 변환 실패")) else Result.success(FamilyAddress(postalCode,address,"",regionCode,37.5,127.0))
        override suspend fun saveAddress(address: FamilyAddress): Result<Unit> {
            saved = address
            return if (saveFails) Result.failure(Exception("저장 실패")) else Result.success(Unit)
        }
    }
    @Test fun failedSaveKeepsInputAndRetriesWithRealRegionAndCoordinates() = runTest(main.testDispatcher) {
        val family = FamilyFake(); val vm = HomeAddressViewModel(family)
        vm.onEvent(FindZipCodeClicked); vm.onEvent(AddressSelected("06134", "서울 서초구 강남대로 143", "1165010800")); advanceUntilIdle()
        vm.onEvent(DetailAddressChanged("810호")); vm.onEvent(RegisterClicked); advanceUntilIdle()
        assertEquals(HomeAddressStep.REGISTER, vm.uiState.value.currentStep)
        assertEquals("810호", vm.uiState.value.form.detailAddress); assertNotNull(vm.uiState.value.error)
        family.saveFails = false; vm.onEvent(RegisterClicked); advanceUntilIdle()
        assertEquals(HomeAddressStep.SAVED, vm.uiState.value.currentStep)
        assertEquals("1165010800", family.saved!!.regionCode); assertEquals(37.5, family.saved!!.latitude, 0.0)
    }
    @Test fun geocodeFailureDoesNotShowSelectedAddressAsReadyToSave() = runTest(main.testDispatcher) {
        val family = FamilyFake().apply { resolveFails = true }; val vm = HomeAddressViewModel(family)
        vm.onEvent(FindZipCodeClicked); vm.onEvent(AddressSelected("06134", "주소", "1165010800")); advanceUntilIdle()
        assertEquals(HomeAddressStep.SEARCH, vm.uiState.value.currentStep); assertFalse(vm.uiState.value.canRegister); assertNull(family.saved)
    }
}
