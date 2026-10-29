package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.repository.AuthRepository
import com.kiri.feature.mypage.model.withdraw.WithdrawUiEffect
import com.kiri.feature.mypage.model.withdraw.WithdrawUiEvent
import com.kiri.feature.mypage.model.withdraw.WithdrawUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<WithdrawUiEffect>(Channel.BUFFERED)
    val effect: Flow<WithdrawUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: WithdrawUiEvent) {
        when (event) {
            WithdrawUiEvent.WithdrawClicked -> withdraw()
            // 탈퇴 요청 중에는 결과를 기다린다
            WithdrawUiEvent.BackClicked -> if (!_uiState.value.isWithdrawing) _effect.trySend(WithdrawUiEffect.NavigateBack)
        }
    }

    private fun withdraw() {
        if (_uiState.value.isWithdrawing) return
        _uiState.update { it.copy(isWithdrawing = true) }
        viewModelScope.launch {
            authRepository.withdraw()
                .onSuccess { _effect.send(WithdrawUiEffect.NavigateToLogin) }
                .onFailure { cause ->
                    _uiState.update { it.copy(isWithdrawing = false) }
                    _effect.send(WithdrawUiEffect.ShowWithdrawError(cause))
                }
        }
    }
}
