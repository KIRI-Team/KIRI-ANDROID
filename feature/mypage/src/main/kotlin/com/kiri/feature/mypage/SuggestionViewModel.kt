package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEffect
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEvent
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState.Companion.MAX_CONTENT_LENGTH
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
class SuggestionViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SuggestionUiState())
    val uiState: StateFlow<SuggestionUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<SuggestionUiEffect>(Channel.BUFFERED)
    val effect: Flow<SuggestionUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: SuggestionUiEvent) {
        when (event) {
            // 500자를 넘는 부분(붙여넣기 포함)은 잘라낸다
            is SuggestionUiEvent.ContentChanged ->
                _uiState.update { it.copy(content = event.content.take(MAX_CONTENT_LENGTH)) }
            SuggestionUiEvent.PhotoAddClicked -> {
                val state = _uiState.value
                if (!state.canAddPhoto) return
                _effect.trySend(SuggestionUiEffect.OpenPhotoPicker(state.remainingPhotoCount))
            }
            is SuggestionUiEvent.PhotosPicked -> _uiState.update { state ->
                // 이미 고른 사진은 다시 넣지 않고, 남은 장수만큼만 붙인다
                val added = event.uris.distinct().filterNot { it in state.photoUris }
                state.copy(photoUris = state.photoUris + added.take(state.remainingPhotoCount))
            }
            is SuggestionUiEvent.PhotoRemoveClicked ->
                _uiState.update { it.copy(photoUris = it.photoUris - event.uri) }
            SuggestionUiEvent.SendClicked -> {
                if (!_uiState.value.canSend) return
                // TODO: 건의 API가 정해지면 내용과 사진을 보내고, 성공했을 때만 돌아간다
                _effect.trySend(SuggestionUiEffect.NavigateBack)
            }
            SuggestionUiEvent.BackClicked -> _effect.trySend(SuggestionUiEffect.NavigateBack)
        }
    }
}
