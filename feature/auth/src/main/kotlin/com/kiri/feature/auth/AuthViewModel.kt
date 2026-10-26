package com.kiri.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.model.auth.SocialProvider
import com.kiri.domain.model.term.Term
import com.kiri.domain.repository.AuthRepository
import com.kiri.domain.repository.TermRepository
import com.kiri.feature.auth.login.NonceGenerator
import com.kiri.feature.auth.login.SocialLoginResult
import com.kiri.feature.auth.model.AuthUiEffect
import com.kiri.feature.auth.model.AuthUiEvent
import com.kiri.feature.auth.model.AuthUiState
import com.kiri.feature.auth.model.LoginProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val termRepository: TermRepository,
    private val nonceGenerator: NonceGenerator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<AuthUiEffect>(Channel.BUFFERED)
    val effect: Flow<AuthUiEffect> = _effect.receiveAsFlow()

    // 지금 진행 중인 SDK 로그인에 넘긴 nonce. 서버 로그인에 같은 값을 보낸다
    private var pendingNonce: String? = null

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.LoginClicked -> startSocialLogin(event.provider)
            is AuthUiEvent.SocialLoginFinished -> finishSocialLogin(event.result)
            AuthUiEvent.TermsRetryClicked -> loadTerms()
            AuthUiEvent.AllTermsToggled -> toggleAllTerms()
            is AuthUiEvent.TermToggled -> toggleTerm(event.term)
            is AuthUiEvent.TermDetailClicked -> openTermDetail(event.term)
            AuthUiEvent.TermDetailDismissed -> _uiState.update { it.copy(viewingTerm = null) }
            // 이미 동의한 약관이어도 해제하지 않고 동의 상태로 두고 창을 닫는다
            is AuthUiEvent.TermDetailAgreed -> _uiState.update {
                it.copy(agreedTermIds = it.agreedTermIds + event.term.id, viewingTerm = null)
            }
            AuthUiEvent.AgreeClicked -> agree()
            AuthUiEvent.BackClicked -> previous()
        }
    }

    // 로그인 중에 다시 누르면 무시한다
    private fun startSocialLogin(provider: LoginProvider) {
        if (_uiState.value.isLoggingIn || _uiState.value.currentStep != AuthStep.LOGIN) return
        val nonce = nonceGenerator.create()
        pendingNonce = nonce
        _uiState.update { it.copy(isLoggingIn = true) }
        _effect.trySend(AuthUiEffect.StartSocialLogin(provider, nonce))
    }

    private fun finishSocialLogin(result: SocialLoginResult) {
        val nonce = pendingNonce ?: return
        pendingNonce = null
        when (result) {
            // 사용자가 창을 닫은 것이라 안내 없이 버튼만 다시 풀어준다
            SocialLoginResult.Cancelled -> _uiState.update { it.copy(isLoggingIn = false) }
            is SocialLoginResult.Failure -> failLogin(result.cause)
            is SocialLoginResult.Success -> loginToServer(result, nonce)
        }
    }

    private fun loginToServer(result: SocialLoginResult.Success, nonce: String) {
        viewModelScope.launch {
            authRepository.login(result.provider.toSocialProvider(), result.token, nonce)
                .onSuccess { login ->
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            isNewUser = login.isNewUser,
                            currentStep = if (login.needsTermsAgreement) AuthStep.TERMS_AGREEMENT else it.currentStep,
                        )
                    }
                    if (login.needsTermsAgreement) loadTerms() else _effect.send(AuthUiEffect.NavigateToMain)
                }
                .onFailure(::failLogin)
        }
    }

    private fun failLogin(cause: Throwable) {
        _uiState.update { it.copy(isLoggingIn = false) }
        _effect.trySend(AuthUiEffect.ShowLoginError(cause))
    }

    // 이미 받았거나 받는 중이면 다시 요청하지 않는다 (로그인으로 돌아갔다 다시 와도 받은 목록을 쓴다)
    private fun loadTerms() {
        val state = _uiState.value
        if (state.currentStep != AuthStep.TERMS_AGREEMENT || state.isLoadingTerms || state.terms.isNotEmpty()) return
        _uiState.update { it.copy(isLoadingTerms = true, termsLoadFailed = false) }
        viewModelScope.launch {
            termRepository.getTerms()
                .onSuccess { terms -> _uiState.update { it.copy(terms = terms, isLoadingTerms = false) } }
                .onFailure { _uiState.update { it.copy(isLoadingTerms = false, termsLoadFailed = true) } }
        }
    }

    private fun toggleAllTerms() {
        _uiState.update {
            it.copy(agreedTermIds = if (it.allAgreed) emptySet() else it.terms.map(Term::id).toSet())
        }
    }

    private fun toggleTerm(term: Term) {
        _uiState.update {
            it.copy(agreedTermIds = if (term.id in it.agreedTermIds) it.agreedTermIds - term.id else it.agreedTermIds + term.id)
        }
    }

    private fun openTermDetail(term: Term) {
        if (_uiState.value.currentStep != AuthStep.TERMS_AGREEMENT) return
        _uiState.update { it.copy(viewingTerm = term) }
    }

    // 저장 중에 다시 누르면 canContinue 가 false 라 무시된다
    private fun agree() {
        val state = _uiState.value
        if (state.currentStep != AuthStep.TERMS_AGREEMENT || !state.canContinue) return
        _uiState.update { it.copy(isAgreeing = true) }
        // 선택 약관에 동의하지 않은 것도 서버에 알린다
        val agreements = state.terms.associate { it.id to (it.id in state.agreedTermIds) }
        viewModelScope.launch {
            termRepository.agree(agreements)
                .onSuccess {
                    _uiState.update { it.copy(isAgreeing = false) }
                    _effect.send(if (state.isNewUser) AuthUiEffect.NavigateToOnboarding else AuthUiEffect.NavigateToMain)
                }
                .onFailure { cause ->
                    _uiState.update { it.copy(isAgreeing = false) }
                    _effect.send(AuthUiEffect.ShowAgreeError(cause))
                }
        }
    }

    // 동의를 저장하는 중에는 로그인으로 돌아가지 않는다 (저장이 끝나면 화면을 옮겨야 해서)
    private fun previous() {
        if (_uiState.value.currentStep != AuthStep.TERMS_AGREEMENT || _uiState.value.isAgreeing) return
        _uiState.update { it.copy(currentStep = AuthStep.LOGIN, viewingTerm = null) }
    }
}

private fun LoginProvider.toSocialProvider(): SocialProvider = when (this) {
    LoginProvider.KAKAO -> SocialProvider.KAKAO
    LoginProvider.GOOGLE -> SocialProvider.GOOGLE
}
