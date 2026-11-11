package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
class HomeAddressViewModel @Inject constructor(
    private val family: com.kiri.domain.repository.FamilyRepository,
    // 등록한 주소 화면의 "OO님의 공간" 에 쓸 가족 이름을 가족 정보 API 로 불러온다
    private val home: com.kiri.domain.home.HomeRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeAddressUiState())
    val uiState: StateFlow<HomeAddressUiState> = _uiState.asStateFlow()

    init {
        home?.let { source ->
            viewModelScope.launch {
                try { source.family().familyName?.let { name -> _uiState.update { it.copy(familyName = name) } } }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { }
            }
        }
    }

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<HomeAddressUiEffect>(Channel.BUFFERED)
    val effect: Flow<HomeAddressUiEffect> = _effect.receiveAsFlow()

    // 주소 검색을 연 작성 단계 (검색을 마치거나 뒤로가면 이 단계로 돌아간다)
    private var selected: com.kiri.domain.model.family.FamilyAddress? = null
    private var formStep = HomeAddressStep.REGISTER

    private var addressJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: HomeAddressUiEvent) {
        if (_uiState.value.saving && !(event == HomeAddressUiEvent.BackClicked && currentStep == HomeAddressStep.SEARCH)) return
        when (event) {
            HomeAddressUiEvent.EditClicked -> edit()
            HomeAddressUiEvent.FindZipCodeClicked -> openSearch()
            is HomeAddressUiEvent.SearchQueryChanged -> {
                if (currentStep != HomeAddressStep.SEARCH) return
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            HomeAddressUiEvent.SearchSubmitted -> Unit
            is HomeAddressUiEvent.AddressSelected -> selectAddress(event.zipCode, event.roadAddress, event.regionCode)
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
        selected = null
        _uiState.update { it.copy(currentStep = HomeAddressStep.EDIT, form = HomeAddress(), error = null) }
    }

    private fun openSearch() {
        if (!isFormStep) return
        formStep = currentStep
        _uiState.update { it.copy(currentStep = HomeAddressStep.SEARCH, searchQuery = "", error = null) }
    }

    // 새 주소를 고르면 이전에 쓴 상세주소는 비운다
    private fun selectAddress(zipCode: String, roadAddress: String, regionCode: String) {
        if (currentStep != HomeAddressStep.SEARCH) return
        val repository = family
        _uiState.update { it.copy(saving = true, error = null) }
        addressJob = viewModelScope.launch {
            repository.resolveAddress(zipCode, roadAddress, regionCode)
                .onSuccess { address ->
                    selected = address
                    _uiState.update { it.copy(currentStep = formStep, form = HomeAddress(address.postalCode, address.address), saving = false) }
                }.onFailure { error -> _uiState.update { it.copy(saving = false, error = error.message ?: "주소 위치를 확인하지 못했어요. 다시 선택해주세요.") } }
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
        val address = selected ?: return
        val repository = family
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.saveAddress(address.copy(detailAddress = state.form.detailAddress.trim()))
                .onSuccess { _uiState.update { it.copy(currentStep = HomeAddressStep.SAVED,
                    savedAddress = state.form.copy(detailAddress = state.form.detailAddress.trim()), form = HomeAddress(), saving = false) } }
                .onFailure { error -> _uiState.update { it.copy(saving = false, error = error.message ?: "주소를 저장하지 못했어요. 입력은 유지했어요.") } }
        }
    }

    private fun previous() {
        when (currentStep) {
            HomeAddressStep.SEARCH -> {
                addressJob?.cancel()
                _uiState.update { it.copy(currentStep = formStep, searchQuery = "", saving = false, error = null) }
            }
            // 수정을 그만두면 작성 중인 주소는 버리고 등록한 주소로 돌아간다
            HomeAddressStep.EDIT -> _uiState.update { it.copy(currentStep = HomeAddressStep.SAVED, form = HomeAddress()) }
            HomeAddressStep.SAVED, HomeAddressStep.REGISTER -> _effect.trySend(HomeAddressUiEffect.NavigateBack)
        }
    }
}
