package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.address.HomeAddress
import com.kiri.feature.mypage.model.address.HomeAddressUiEffect
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent
import com.kiri.feature.mypage.model.common.MypageProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAddressViewModelTest {

    @get:org.junit.Rule val mainDispatcherRule = MainDispatcherRule(kotlinx.coroutines.test.UnconfinedTestDispatcher())
    private val family = object : com.kiri.domain.repository.FamilyRepository {
        override suspend fun createFamily(name: String): Result<com.kiri.domain.model.family.Family> = error("unused")
        override suspend fun renameFamily(name: String) = Result.success(name)
        override suspend fun resolveAddress(postalCode: String, address: String, regionCode: String) =
            Result.success(com.kiri.domain.model.family.FamilyAddress(postalCode, address, "", "1165010800", 37.5, 127.0))
        override suspend fun saveAddress(address: com.kiri.domain.model.family.FamilyAddress) = Result.success(Unit)
    }
    private val viewModel = HomeAddressViewModel(family)
    private val state get() = viewModel.uiState.value

    private fun selectAddress(zipCode: String = "06134", roadAddress: String = "서울 서초구 강남대로 143") {
        viewModel.onEvent(HomeAddressUiEvent.FindZipCodeClicked)
        viewModel.onEvent(HomeAddressUiEvent.AddressSelected(zipCode, roadAddress))
    }

    private fun registerAddress() {
        selectAddress()
        viewModel.onEvent(HomeAddressUiEvent.DetailAddressChanged("810-603"))
        viewModel.onEvent(HomeAddressUiEvent.RegisterClicked)
    }

    @Test
    fun 주소가_없으면_등록_화면에서_시작하고_등록할_수_없다() {
        assertEquals(HomeAddressStep.REGISTER, state.currentStep)
        assertEquals("", state.familyName)
        assertNull(state.savedAddress)
        assertFalse(state.canRegister)
    }

    @Test
    fun 우편번호_찾기를_누르면_검색_화면으로_이동하고_뒤로가면_등록_화면으로_돌아온다() {
        viewModel.onEvent(HomeAddressUiEvent.FindZipCodeClicked)
        assertEquals(HomeAddressStep.SEARCH, state.currentStep)

        viewModel.onEvent(HomeAddressUiEvent.SearchQueryChanged("강남대로"))
        assertEquals("강남대로", state.searchQuery)

        viewModel.onEvent(HomeAddressUiEvent.BackClicked)
        assertEquals(HomeAddressStep.REGISTER, state.currentStep)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun 주소를_고르면_우편번호와_주소를_채우고_등록할_수_있다() {
        selectAddress()

        assertEquals(HomeAddressStep.REGISTER, state.currentStep)
        assertEquals(HomeAddress(zipCode = "06134", roadAddress = "서울 서초구 강남대로 143"), state.form)
        assertTrue(state.canRegister)
    }

    @Test
    fun 주소를_고르기_전에는_상세주소를_입력할_수_없다() {
        viewModel.onEvent(HomeAddressUiEvent.DetailAddressChanged("810-603"))

        assertEquals("", state.form.detailAddress)
    }

    @Test
    fun 다른_주소를_고르면_이전_상세주소를_비운다() {
        selectAddress()
        viewModel.onEvent(HomeAddressUiEvent.DetailAddressChanged("810-603"))

        selectAddress(zipCode = "04524", roadAddress = "서울 중구 남대문로9길 40")

        assertEquals("", state.form.detailAddress)
        assertEquals("04524", state.form.zipCode)
    }

    @Test
    fun 주소가_비어_있으면_등록하기를_눌러도_등록되지_않는다() {
        viewModel.onEvent(HomeAddressUiEvent.RegisterClicked)

        assertEquals(HomeAddressStep.REGISTER, state.currentStep)
        assertNull(state.savedAddress)
    }

    @Test
    fun 등록하면_주소_보기_화면으로_이동하고_작성칸을_비운다() {
        registerAddress()

        assertEquals(HomeAddressStep.SAVED, state.currentStep)
        assertEquals(HomeAddress("06134", "서울 서초구 강남대로 143", "810-603"), state.savedAddress)
        assertEquals("서울 서초구 강남대로 143, 810-603", state.savedAddress?.displayText)
        assertEquals(HomeAddress(), state.form)
    }

    @Test
    fun 상세주소가_없어도_등록할_수_있고_한_줄_주소에는_주소만_보인다() {
        selectAddress()
        viewModel.onEvent(HomeAddressUiEvent.DetailAddressChanged("   "))
        viewModel.onEvent(HomeAddressUiEvent.RegisterClicked)

        assertEquals("", state.savedAddress?.detailAddress)
        assertEquals("서울 서초구 강남대로 143", state.savedAddress?.displayText)
    }

    @Test
    fun 수정하기를_누르면_빈_작성칸으로_수정_화면을_연다() {
        registerAddress()

        viewModel.onEvent(HomeAddressUiEvent.EditClicked)

        assertEquals(HomeAddressStep.EDIT, state.currentStep)
        assertEquals(HomeAddress(), state.form)
        assertFalse(state.canRegister)
    }

    @Test
    fun 수정_화면에서_검색하면_수정_화면으로_돌아오고_등록하면_주소를_바꾼다() {
        registerAddress()
        viewModel.onEvent(HomeAddressUiEvent.EditClicked)

        selectAddress(zipCode = "04524", roadAddress = "서울 중구 남대문로9길 40")
        assertEquals(HomeAddressStep.EDIT, state.currentStep)

        viewModel.onEvent(HomeAddressUiEvent.RegisterClicked)

        assertEquals(HomeAddressStep.SAVED, state.currentStep)
        assertEquals(HomeAddress("04524", "서울 중구 남대문로9길 40"), state.savedAddress)
    }

    @Test
    fun 수정_화면에서_뒤로가면_작성한_내용을_버리고_기존_주소로_돌아간다() {
        registerAddress()
        viewModel.onEvent(HomeAddressUiEvent.EditClicked)
        selectAddress(zipCode = "04524", roadAddress = "서울 중구 남대문로9길 40")

        viewModel.onEvent(HomeAddressUiEvent.BackClicked)

        assertEquals(HomeAddressStep.SAVED, state.currentStep)
        assertEquals("06134", state.savedAddress?.zipCode)
        assertEquals(HomeAddress(), state.form)
    }

    @Test
    fun 주소가_없을_때는_수정하기가_무시된다() {
        viewModel.onEvent(HomeAddressUiEvent.EditClicked)

        assertEquals(HomeAddressStep.REGISTER, state.currentStep)
    }

    @Test
    fun 검색_화면이_아니면_주소_선택이_무시된다() = runTest {
        viewModel.onEvent(HomeAddressUiEvent.AddressSelected("06134", "서울 서초구 강남대로 143"))

        assertEquals(HomeAddress(), state.form)
    }

    @Test
    fun 등록_화면에서_뒤로가면_마이페이지로_돌아간다() = runTest {
        viewModel.onEvent(HomeAddressUiEvent.BackClicked)

        assertEquals(HomeAddressUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 주소_보기_화면에서_뒤로가면_마이페이지로_돌아간다() = runTest {
        registerAddress()

        viewModel.onEvent(HomeAddressUiEvent.BackClicked)

        assertEquals(HomeAddressUiEffect.NavigateBack, viewModel.effect.first())
    }

    @Test
    fun 검색_화면에서_뒤로가면_화면_이동_effect를_보내지_않는다() = runTest {
        viewModel.onEvent(HomeAddressUiEvent.FindZipCodeClicked)

        viewModel.onEvent(HomeAddressUiEvent.BackClicked)

        assertNull(withTimeoutOrNull(100) { viewModel.effect.first() })
    }
}
