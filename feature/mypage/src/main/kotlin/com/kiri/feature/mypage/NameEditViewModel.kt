package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import com.kiri.domain.model.nickname.NicknameValidator
import com.kiri.feature.mypage.model.nameedit.NameEditUiEffect
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import com.kiri.feature.mypage.model.nameedit.NameEditUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

// 닉네임·가족명 수정이 함께 쓰는 동작. 둘 다 온보딩과 같은 규칙을 따르고 최대 길이만 다르다
abstract class NameEditViewModel(
    currentName: String,
    maxLength: Int = NicknameValidator.MAX_LENGTH,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NameEditUiState(currentName = currentName, maxLength = maxLength))
    val uiState: StateFlow<NameEditUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<NameEditUiEffect>(Channel.BUFFERED)
    val effect: Flow<NameEditUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: NameEditUiEvent) {
        when (event) {
            is NameEditUiEvent.NameChanged -> _uiState.update {
                it.copy(name = event.name, violations = emptySet())
            }
            NameEditUiEvent.NextClicked -> next()
            NameEditUiEvent.BackClicked -> _effect.trySend(NameEditUiEffect.NavigateBack)
        }
    }

    // 규칙을 지키지 못하면 오류를 표시하고 머문다
    private fun next() {
        val violations = NicknameValidator.violations(_uiState.value.name, _uiState.value.maxLength)
        _uiState.update { it.copy(violations = violations) }
        if (violations.isNotEmpty()) return
        // TODO: 수정 API가 정해지면 저장 후 이동 (월 1회 제한도 서버 응답으로 처리)
        _effect.trySend(NameEditUiEffect.NavigateBack)
    }
}
