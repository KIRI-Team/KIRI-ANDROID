package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import com.kiri.feature.mypage.model.license.OpenLicenseUiEffect
import com.kiri.feature.mypage.model.license.OpenLicenseUiEvent
import com.kiri.feature.mypage.model.license.OpenLicenseUiState
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
class OpenLicenseViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(OpenLicenseUiState())
    val uiState: StateFlow<OpenLicenseUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<OpenLicenseUiEffect>(Channel.BUFFERED)
    val effect: Flow<OpenLicenseUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: OpenLicenseUiEvent) {
        when (event) {
            is OpenLicenseUiEvent.LicenseClicked -> _uiState.update { it.copy(viewingLicense = event.license) }
            OpenLicenseUiEvent.LicenseDialogClosed -> _uiState.update { it.copy(viewingLicense = null) }
            OpenLicenseUiEvent.BackClicked -> _effect.trySend(OpenLicenseUiEffect.NavigateBack)
        }
    }
}
