package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.model.taste.Taste
import com.kiri.domain.repository.TasteRepository
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEffect
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEvent
import com.kiri.feature.mypage.model.preference.MyPreferenceUiState
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

// 내 취향 설정. 저장된 취향을 불러와 보여주고, 고친 취향을 저장한다
// 선호 활동은 최대 3개, 나머지 항목은 하나씩 고르고, 이미 고른 칩을 다시 누르면 선택을 해제한다
@HiltViewModel
class MyPreferenceViewModel @Inject constructor(
    private val tasteRepository: TasteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPreferenceUiState(isLoading = true))
    val uiState: StateFlow<MyPreferenceUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<MyPreferenceUiEffect>(Channel.BUFFERED)
    val effect: Flow<MyPreferenceUiEffect> = _effect.receiveAsFlow()

    init {
        // 불러오지 못하면 비어 있는 채로 두고 알린다 (새로 골라 저장할 수 있다)
        viewModelScope.launch {
            tasteRepository.getMyTaste()
                .onSuccess { myTaste -> _uiState.update { it.withTaste(myTaste).copy(isLoading = false) } }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.trySend(MyPreferenceUiEffect.ShowLoadError)
                }
        }
    }

    fun onEvent(event: MyPreferenceUiEvent) {
        when (event) {
            MyPreferenceUiEvent.SaveClicked -> save()
            MyPreferenceUiEvent.BackClicked -> _effect.trySend(MyPreferenceUiEffect.NavigateBack)
            // 불러오는 중이나 저장 중에는 고른 값이 덮이거나 바뀌지 않도록 무시한다
            else -> if (!_uiState.value.isLoading && !_uiState.value.isSaving) _uiState.update { it.select(event) }
        }
    }

    private fun MyPreferenceUiState.select(event: MyPreferenceUiEvent): MyPreferenceUiState = when (event) {
        is MyPreferenceUiEvent.CategoryClicked -> when {
            event.category in categories -> copy(categories = categories - event.category)
            // 이미 3개를 골랐으면 더 고르지 않는다
            categories.size >= Taste.MAX_CATEGORIES -> this
            else -> copy(categories = categories + event.category)
        }
        is MyPreferenceUiEvent.SpaceClicked -> copy(space = event.space.takeUnless { it == space })
        is MyPreferenceUiEvent.ActivityClicked -> copy(activity = event.activity.takeUnless { it == activity })
        is MyPreferenceUiEvent.PriceClicked -> copy(price = event.price.takeUnless { it == price })
        is MyPreferenceUiEvent.DurationClicked -> copy(duration = event.duration.takeUnless { it == duration })
        MyPreferenceUiEvent.SaveClicked, MyPreferenceUiEvent.BackClicked -> this
    }

    // 저장하면 마이페이지로 돌아가고, 실패하면 고른 값을 그대로 두고 알린다
    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            tasteRepository.saveMyTaste(state.toTaste())
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.trySend(MyPreferenceUiEffect.Saved)
                }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.trySend(MyPreferenceUiEffect.ShowSaveError)
                }
        }
    }
}
