package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import com.kiri.feature.mypage.model.notification.NotificationSettingUiEffect
import com.kiri.feature.mypage.model.notification.NotificationSettingUiEvent
import com.kiri.feature.mypage.model.notification.NotificationSettingUiState
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
class NotificationSettingViewModel @Inject constructor() : ViewModel() {
    // TODO: 알림 설정 API가 정해지면 저장된 설정을 불러오고 바꿀 때마다 저장 (현재는 ViewModel 안에만 보관)
    private val _uiState = MutableStateFlow(NotificationSettingUiState())
    val uiState: StateFlow<NotificationSettingUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<NotificationSettingUiEffect>(Channel.BUFFERED)
    val effect: Flow<NotificationSettingUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: NotificationSettingUiEvent) {
        when (event) {
            NotificationSettingUiEvent.AllToggled -> _uiState.update { it.copy(allEnabled = !it.allEnabled) }
            NotificationSettingUiEvent.RegularToggled -> {
                if (!_uiState.value.canChangeRegular) return
                _uiState.update { it.copy(regularEnabled = !it.regularEnabled) }
            }
            NotificationSettingUiEvent.RegularTimeClicked -> {
                if (!_uiState.value.canChangeRegularTime) return
                _uiState.update { it.copy(timeDialogVisible = true) }
            }
            is NotificationSettingUiEvent.RegularTimeSaved -> _uiState.update {
                // 초 단위는 고를 수 없으니 분까지만 남긴다
                it.copy(regularTime = event.time.withSecond(0).withNano(0), timeDialogVisible = false)
            }
            NotificationSettingUiEvent.TimeDialogDismissed -> _uiState.update { it.copy(timeDialogVisible = false) }
            NotificationSettingUiEvent.BackClicked -> _effect.trySend(NotificationSettingUiEffect.NavigateBack)
        }
    }
}
