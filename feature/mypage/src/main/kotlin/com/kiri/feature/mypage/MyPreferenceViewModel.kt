package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import com.kiri.feature.mypage.model.common.MypageProfile
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

// 내 취향 설정. 선호 활동은 최대 3개, 나머지 항목은 하나씩 고른다
// 취향은 필수가 아니라 이미 고른 칩을 다시 누르면 선택을 해제한다
// TODO: 취향 조회 API가 연결되면 저장된 취향과 닉네임을 불러와 채우고, 저장 API가 정해지면 저장
@HiltViewModel
class MyPreferenceViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(MyPreferenceUiState(nickname = MypageProfile.Sample.nickname))
    val uiState: StateFlow<MyPreferenceUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<MyPreferenceUiEffect>(Channel.BUFFERED)
    val effect: Flow<MyPreferenceUiEffect> = _effect.receiveAsFlow()

    fun onEvent(event: MyPreferenceUiEvent) {
        when (event) {
            is MyPreferenceUiEvent.CategoryClicked -> _uiState.update { state ->
                val categories = state.categories
                when {
                    event.category in categories -> state.copy(categories = categories - event.category)
                    // 이미 3개를 골랐으면 더 고르지 않는다
                    categories.size >= MyPreferenceUiState.MAX_CATEGORIES -> state
                    else -> state.copy(categories = categories + event.category)
                }
            }
            is MyPreferenceUiEvent.SpaceClicked -> _uiState.update {
                it.copy(space = event.space.takeUnless { space -> space == it.space })
            }
            is MyPreferenceUiEvent.ActivityClicked -> _uiState.update {
                it.copy(activity = event.activity.takeUnless { activity -> activity == it.activity })
            }
            is MyPreferenceUiEvent.PriceClicked -> _uiState.update {
                it.copy(price = event.price.takeUnless { price -> price == it.price })
            }
            is MyPreferenceUiEvent.DurationClicked -> _uiState.update {
                it.copy(duration = event.duration.takeUnless { duration -> duration == it.duration })
            }
            // TODO: 저장 API가 정해지면 저장 후 이동
            MyPreferenceUiEvent.SaveClicked -> Unit
            MyPreferenceUiEvent.BackClicked -> _effect.trySend(MyPreferenceUiEffect.NavigateBack)
        }
    }
}
