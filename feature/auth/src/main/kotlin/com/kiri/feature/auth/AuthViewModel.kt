package com.kiri.feature.auth

import androidx.lifecycle.ViewModel
import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.auth.model.AuthUiEffect
import com.kiri.feature.auth.model.AuthUiEvent
import com.kiri.feature.auth.model.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<AuthUiEffect>(Channel.BUFFERED)
    val effect: Flow<AuthUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            // TODO: 소셜 로그인 연동 후 로그인 성공 시에만 약관동의로 이동 (현재는 UI 흐름 확인용으로 바로 이동)
            is AuthUiEvent.LoginClicked -> moveTo(AuthStep.TERMS_AGREEMENT)
            AuthUiEvent.AllTermsToggled -> toggleAllTerms()
            is AuthUiEvent.TermToggled -> toggleTerm(event.term)
            is AuthUiEvent.TermDetailClicked -> openTermDetail(event.term)
            AuthUiEvent.TermDetailDismissed -> _uiState.update { it.copy(viewingTerm = null) }
            // 이미 동의한 약관이어도 해제하지 않고 동의 상태로 두고 창을 닫는다
            is AuthUiEvent.TermDetailAgreed -> _uiState.update {
                it.copy(agreedTerms = it.agreedTerms + event.term, viewingTerm = null)
            }
            AuthUiEvent.AgreeClicked -> agree()
            AuthUiEvent.BackClicked -> previous()
        }
    }

    private fun moveTo(step: AuthStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    private fun toggleAllTerms() {
        _uiState.update {
            it.copy(agreedTerms = if (it.allAgreed) emptySet() else AgreementTerm.entries.toSet())
        }
    }

    private fun toggleTerm(term: AgreementTerm) {
        _uiState.update {
            it.copy(agreedTerms = if (term in it.agreedTerms) it.agreedTerms - term else it.agreedTerms + term)
        }
    }

    private fun openTermDetail(term: AgreementTerm) {
        if (_uiState.value.currentStep != AuthStep.TERMS_AGREEMENT) return
        _uiState.update { it.copy(viewingTerm = term) }
    }

    private fun agree() {
        if (!_uiState.value.canContinue) return
        _effect.trySend(AuthUiEffect.NavigateToOnboarding)
    }

    private fun previous() {
        if (_uiState.value.currentStep != AuthStep.TERMS_AGREEMENT) return
        _uiState.update { it.copy(currentStep = AuthStep.LOGIN, viewingTerm = null) }
    }
}
