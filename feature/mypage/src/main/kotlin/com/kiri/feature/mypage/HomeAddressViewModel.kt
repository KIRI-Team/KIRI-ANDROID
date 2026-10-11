package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import com.kiri.feature.mypage.model.address.HomeAddress
import com.kiri.feature.mypage.model.address.HomeAddressUiEffect
import com.kiri.feature.mypage.model.address.HomeAddressUiEvent
import com.kiri.feature.mypage.model.address.HomeAddressUiState
import com.kiri.feature.mypage.model.common.MypageProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class HomeAddressViewModel @Inject constructor() : ViewModel() {
    // TODO: 주소 저장 API가 생기면 저장된 주소를 불러오고, 등록·수정 시 서버에 저장 (현재는 ViewModel 안에만 보관)
    private val _uiState = MutableStateFlow(HomeAddressUiState(familyName = MypageProfile.Sample.familyName))
    val uiState: StateFlow<HomeAddressUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<HomeAddressUiEffect>(Channel.BUFFERED)
    val effect: Flow<HomeAddressUiEffect> = _effect.receiveAsFlow()

    // 주소 검색을 연 작성 단계 (검색을 마치거나 뒤로가면 이 단계로 돌아간다)
    private var formStep = HomeAddressStep.REGISTER

    fun onEvent(event: HomeAddressUiEvent) {
        when (event) {
            HomeAddressUiEvent.EditClicked -> edit()
            HomeAddressUiEvent.FindZipCodeClicked -> openSearch()
            is HomeAddressUiEvent.SearchQueryChanged -> {
                if (currentStep != HomeAddressStep.SEARCH) return
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            // TODO: 주소 검색 API와 결과 목록 디자인이 정해지면 검색 결과를 보여주고, 고르면 AddressSelected 로 전달
            HomeAddressUiEvent.SearchSubmitted -> Unit
            is HomeAddressUiEvent.AddressSelected -> selectAddress(event.zipCode, event.roadAddress)
            is HomeAddressUiEvent.DetailAddressChanged -> changeDetailAddress(event.detailAddress)
            HomeAddressUiEvent.RegisterClicked -> register()
            HomeAddressUiEvent.BackClicked -> previous()
        }
    }

    private val currentStep get() = _uiState.value.currentStep

    private val isFormStep get() = currentStep == HomeAddressStep.REGISTER || currentStep == HomeAddressStep.EDIT

    // 수정은 빈 칸에서 새로 작성한다
    private fun edit() {
        if (currentStep != HomeAddressStep.SAVED) return
        _uiState.update { it.copy(currentStep = HomeAddressStep.EDIT, form = HomeAddress()) }
    }

    private fun openSearch() {
        if (!isFormStep) return
        formStep = currentStep
        _uiState.update { it.copy(currentStep = HomeAddressStep.SEARCH, searchQuery = "") }
    }

    // 새 주소를 고르면 이전에 쓴 상세주소는 비운다
    private fun selectAddress(zipCode: String, roadAddress: String) {
        if (currentStep != HomeAddressStep.SEARCH) return
        _uiState.update {
            it.copy(currentStep = formStep, form = HomeAddress(zipCode = zipCode, roadAddress = roadAddress))
        }
    }

    // 상세주소는 주소를 고른 뒤에만 입력할 수 있다
    private fun changeDetailAddress(detailAddress: String) {
        if (!isFormStep || _uiState.value.form.roadAddress.isBlank()) return
        _uiState.update { it.copy(form = it.form.copy(detailAddress = detailAddress)) }
    }

    private fun register() {
        val state = _uiState.value
        if (!isFormStep || !state.canRegister) return
        _uiState.update {
            it.copy(
                currentStep = HomeAddressStep.SAVED,
                savedAddress = it.form.copy(detailAddress = it.form.detailAddress.trim()),
                form = HomeAddress(),
            )
        }
    }

    private fun previous() {
        when (currentStep) {
            HomeAddressStep.SEARCH -> _uiState.update { it.copy(currentStep = formStep, searchQuery = "") }
            // 수정을 그만두면 작성 중인 주소는 버리고 등록한 주소로 돌아간다
            HomeAddressStep.EDIT -> _uiState.update { it.copy(currentStep = HomeAddressStep.SAVED, form = HomeAddress()) }
            HomeAddressStep.SAVED, HomeAddressStep.REGISTER -> _effect.trySend(HomeAddressUiEffect.NavigateBack)
        }
    }
}
