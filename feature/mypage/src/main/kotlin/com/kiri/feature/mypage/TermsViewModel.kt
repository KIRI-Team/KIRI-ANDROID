package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.repository.TermRepository
import com.kiri.feature.mypage.model.terms.TermsUiEffect
import com.kiri.feature.mypage.model.terms.TermsUiEvent
import com.kiri.feature.mypage.model.terms.TermsUiState
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

// 이용 약관. 회원가입 약관 동의와 같은 약관 목록 API 로 약관을 불러온다
@HiltViewModel
class TermsViewModel @Inject constructor(
    private val termRepository: TermRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TermsUiState())
    val uiState: StateFlow<TermsUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<TermsUiEffect>(Channel.BUFFERED)
    val effect: Flow<TermsUiEffect> = _effect.receiveAsFlow()

    init {
        loadTerms()
    }

    fun onEvent(event: TermsUiEvent) {
        when (event) {
            is TermsUiEvent.TermClicked -> _uiState.update { it.copy(viewingTerm = event.term) }
            TermsUiEvent.TermDetailClosed -> _uiState.update { it.copy(viewingTerm = null) }
            TermsUiEvent.RetryClicked -> loadTerms()
            TermsUiEvent.BackClicked -> _effect.trySend(TermsUiEffect.NavigateBack)
        }
    }

    // 불러오는 중에 다시 시도를 눌러도 한 번만 요청한다
    private fun loadTerms() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            termRepository.getTerms()
                .onSuccess { terms -> _uiState.update { it.copy(terms = terms, isLoading = false) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
        }
    }
}
