package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

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

    private var isSaving = false

    // 바꾼 이름을 저장한다. 서버에 저장하지 않는 화면은 바로 성공한다
    protected open suspend fun save(name: String): Result<Unit> = Result.success(Unit)

    // 저장 실패가 이미 있는 이름 때문인지. 그렇다면 입력창 아래에 따로 안내한다
    protected open fun isDuplicated(error: Throwable): Boolean = false

    fun onEvent(event: NameEditUiEvent) {
        when (event) {
            is NameEditUiEvent.NameChanged -> _uiState.update {
                it.copy(name = event.name, violations = emptySet(), isDuplicated = false)
            }
            NameEditUiEvent.NextClicked -> next()
            NameEditUiEvent.BackClicked -> _effect.trySend(NameEditUiEffect.NavigateBack)
        }
    }

    // 규칙을 지키지 못하면 오류를 표시하고 머문다. 저장하면 이전 화면으로 돌아가고, 저장하지 못하면 머물며 알린다
    // 저장 중에 다시 누르면 무시한다
    private fun next() {
        if (isSaving) return
        val name = _uiState.value.name
        val violations = NicknameValidator.violations(name, _uiState.value.maxLength)
        _uiState.update { it.copy(violations = violations) }
        if (violations.isNotEmpty()) return
        isSaving = true
        viewModelScope.launch {
            save(name)
                .onSuccess { _effect.trySend(NameEditUiEffect.NavigateBack) }
                .onFailure { error ->
                    if (isDuplicated(error)) {
                        _uiState.update { it.copy(isDuplicated = true) }
                    } else {
                        _effect.trySend(NameEditUiEffect.ShowSaveError)
                    }
                }
            isSaving = false
        }
    }
}
