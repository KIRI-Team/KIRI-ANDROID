package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.repository.ReviewPhotoRepository
import com.kiri.domain.repository.SuggestionRepository
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEffect
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEvent
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState.Companion.MAX_CONTENT_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 건의를 서버에 보낸다 (POST api/v1/suggestions). 사진이 있으면 먼저 올리고 받은 이미지 키를 함께 보낸다
// 매개변수에 모두 기본값을 주면 Kotlin 이 인자 없는 생성자를 하나 더 만들어 Hilt 가 생성자를 고르지 못한다.
// 그래서 기본값 없이 받는다 (테스트는 null 을 넘겨 서버 없이 쓸 수 있다)
@HiltViewModel
class SuggestionViewModel @Inject constructor(
    private val suggestions: SuggestionRepository?,
    private val photos: ReviewPhotoRepository?,
) : ViewModel() {
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
            SuggestionUiEvent.SendClicked -> send()
            SuggestionUiEvent.BackClicked -> _effect.trySend(SuggestionUiEffect.NavigateBack)
        }
    }

    // 보내는 중에는 다시 누를 수 없다. 성공했을 때만 돌아가고, 실패하면 적은 내용과 사진을 그대로 두고 안내한다
    private fun send() {
        val state = _uiState.value
        if (!state.canSend) return
        val repository = suggestions
        if (repository == null) {
            _effect.trySend(SuggestionUiEffect.NavigateBack)
            return
        }
        _uiState.update { it.copy(sending = true) }
        viewModelScope.launch {
            try {
                val imageKey = state.photoUris.firstOrNull()?.let { uri ->
                    val uploader = photos ?: return@let null
                    uploader.upload(uri).getOrElse { return@launch fail(PHOTO_FAILED_MESSAGE) }
                }
                repository.send(state.content.trim(), imageKey).getOrElse { return@launch fail(SEND_FAILED_MESSAGE) }
                _uiState.update { it.copy(sending = false) }
                _effect.trySend(SuggestionUiEffect.ShowMessage(SENT_MESSAGE))
                _effect.trySend(SuggestionUiEffect.NavigateBack)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                fail(SEND_FAILED_MESSAGE)
            }
        }
    }

    private fun fail(message: String) {
        _uiState.update { it.copy(sending = false) }
        _effect.trySend(SuggestionUiEffect.ShowMessage(message))
    }

    companion object {
        const val SENT_MESSAGE = "소중한 의견 감사해요!"
        const val PHOTO_FAILED_MESSAGE = "사진을 올리지 못했어요. 다시 시도해주세요."
        const val SEND_FAILED_MESSAGE = "의견을 보내지 못했어요. 다시 시도해주세요."
    }
}
