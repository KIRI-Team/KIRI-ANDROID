package com.kiri.feature.onboarding

import androidx.lifecycle.ViewModel
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import com.kiri.feature.onboarding.model.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OnboardingViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<OnboardingUiEvent>()
    val event: SharedFlow<OnboardingUiEvent> = _event.asSharedFlow()
}