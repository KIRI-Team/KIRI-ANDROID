package com.kiri.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.feature.onboarding.model.OnboardingUiEffect
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import com.kiri.feature.onboarding.model.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 온보딩 메인에서 선택하는 이후 진행 경로
enum class OnboardingPath {
    CREATE_FAMILY,
    JOIN_FAMILY,
    BROWSE,
}

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<OnboardingUiEffect>()
    val effect: SharedFlow<OnboardingUiEffect> = _effect.asSharedFlow()

    private var selectedPath: OnboardingPath? = null
    private var loadingJob: Job? = null

    fun initOnboarding() {
        selectedPath = null
        _uiState.update { it.copy(currentStep = OnboardingStep.INPUT_NICKNAME) }
    }

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.NicknameChanged -> _uiState.update { it.copy(nickname = event.nickname) }
            is OnboardingUiEvent.RoleSelected -> _uiState.update { it.copy(selectedRole = event.role) }
            is OnboardingUiEvent.PathSelected -> selectPath(event.path)
            is OnboardingUiEvent.FamilyNameChanged -> _uiState.update { it.copy(familyName = event.familyName) }
            is OnboardingUiEvent.InputFamilyCodeChanged -> _uiState.update { it.copy(inputFamilyCode = event.code) }
            OnboardingUiEvent.NextClicked -> next()
            OnboardingUiEvent.BackClicked -> previous()
        }
    }

    // 온보딩 메인에서 경로를 선택하면 해당 경로의 첫 단계로 이동
    private fun selectPath(path: OnboardingPath) {
        if (_uiState.value.currentStep != OnboardingStep.ONBOARDING_MAIN) return
        selectedPath = path
        next()
    }

    private fun next() {
        val currentStep = _uiState.value.currentStep
        val steps = stepsOf(selectedPath)
        // 메인에서 경로를 선택하기 전이거나 마지막 단계면 이동하지 않음
        val nextStep = steps.getOrNull(steps.indexOf(currentStep) + 1) ?: return
        _uiState.update { it.copy(currentStep = nextStep) }
        if (nextStep == OnboardingStep.GENERATE_FAMILY_CODE_LOADING) startLoading()
    }

    // TODO: 코드 생성 로직이 추가되면 실제 생성 완료 시점에 넘어가도록 교체 (지금은 임시로 1초 대기)
    private fun startLoading() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            delay(LOADING_DURATION_MILLIS)
            if (_uiState.value.currentStep == OnboardingStep.GENERATE_FAMILY_CODE_LOADING) {
                _uiState.update { it.copy(familyCode = TemporaryFamilyCode.code) }
                next()
            }
        }
    }

    private fun previous() {
        val currentStep = _uiState.value.currentStep
        val steps = stepsOf(selectedPath)
        // 로딩 단계는 뒤로가기로 돌아오지 않도록 건너뜀
        val previousStep = steps
            .take(steps.indexOf(currentStep).coerceAtLeast(0))
            .lastOrNull { it != OnboardingStep.GENERATE_FAMILY_CODE_LOADING }
            ?: return

        loadingJob?.cancel()
        // 메인으로 돌아오면 선택한 경로 초기화
        if (previousStep == OnboardingStep.ONBOARDING_MAIN) selectedPath = null
        _uiState.update { it.copy(currentStep = previousStep) }
    }

    private fun stepsOf(path: OnboardingPath?): List<OnboardingStep> = COMMON_STEPS + when (path) {
        OnboardingPath.CREATE_FAMILY -> listOf(
            OnboardingStep.INPUT_FAMILY_NAME,
            OnboardingStep.GENERATE_FAMILY_CODE_LOADING,
            OnboardingStep.GENERATE_FAMILY_CODE,
        )
        OnboardingPath.JOIN_FAMILY -> listOf(OnboardingStep.INPUT_FAMILY_CODE)
        // TODO: 둘러보기 단계가 OnboardingStep에 추가되면 연결
        OnboardingPath.BROWSE, null -> emptyList()
    }

    private companion object {
        const val LOADING_DURATION_MILLIS = 1_000L

        val COMMON_STEPS = listOf(
            OnboardingStep.INPUT_NICKNAME,
            OnboardingStep.CHOOSE_ROLE,
            OnboardingStep.ONBOARDING_MAIN,
        )
    }
}