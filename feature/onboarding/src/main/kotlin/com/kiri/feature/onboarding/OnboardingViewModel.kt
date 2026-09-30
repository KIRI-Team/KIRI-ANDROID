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

class OnboardingViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<OnboardingUiEvent>()
    val event: SharedFlow<OnboardingUiEvent> = _event.asSharedFlow()

    fun initOnboarding() {
        _uiState.update { it.copy(currentStep = OnboardingStep.INPUT_NICKNAME) }
    }

    fun next() {
        _uiState.update {
            it.copy(
                currentStep = when (this) {
                    OnboardingStep.INPUT_NICKNAME -> OnboardingStep.CHOOSE_ROLE
                    OnboardingStep.CHOOSE_ROLE -> OnboardingStep.ONBOARDING_MAIN
                    else -> Unit
                }
            )
        }
    }
}