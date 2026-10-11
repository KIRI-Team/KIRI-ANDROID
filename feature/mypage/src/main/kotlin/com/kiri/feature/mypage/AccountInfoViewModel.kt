package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import com.kiri.feature.mypage.model.account.AccountInfoUiEffect
import com.kiri.feature.mypage.model.account.AccountInfoUiEvent
import com.kiri.feature.mypage.model.account.AccountInfoUiState
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
class AccountInfoViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AccountInfoUiState())
    val uiState: StateFlow<AccountInfoUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<AccountInfoUiEffect>(Channel.BUFFERED)
    val effect: Flow<AccountInfoUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: AccountInfoUiEvent) {
        when (event) {
            AccountInfoUiEvent.TermsClicked -> _effect.trySend(AccountInfoUiEffect.NavigateToTerms)
            AccountInfoUiEvent.OpenLicenseClicked -> _effect.trySend(AccountInfoUiEffect.NavigateToOpenLicense)
            AccountInfoUiEvent.LogoutClicked -> _uiState.update { it.copy(isLogoutDialogVisible = true) }
            AccountInfoUiEvent.LogoutCanceled -> _uiState.update { it.copy(isLogoutDialogVisible = false) }
            AccountInfoUiEvent.LogoutConfirmed -> {
                if (!_uiState.value.isLogoutDialogVisible) return
                _uiState.update { it.copy(isLogoutDialogVisible = false) }
                // TODO: 로그아웃 API·토큰 저장소가 생기면 로그아웃을 처리한 뒤 이동한다
                _effect.trySend(AccountInfoUiEffect.NavigateToLogin)
            }
            AccountInfoUiEvent.WithdrawClicked -> _effect.trySend(AccountInfoUiEffect.NavigateToWithdraw)
            AccountInfoUiEvent.BackClicked -> _effect.trySend(AccountInfoUiEffect.NavigateBack)
        }
    }
}
