package com.kiri.feature.onboarding.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.onboarding.OnboardingPath
import com.kiri.feature.onboarding.OnboardingStep
import com.kiri.feature.onboarding.OnboardingViewModel
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import com.kiri.feature.onboarding.model.OnboardingUiState

@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OnboardingScreen(
        modifier = modifier,
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBackClick = { onEvent(OnboardingUiEvent.BackClicked) }

    // 첫 단계에서는 비활성화해서 시스템 뒤로가기로 앱을 종료할 수 있게 한다
    BackHandler(enabled = state.currentStep != OnboardingStep.INPUT_NICKNAME, onBack = onBackClick)

    Box(modifier.testTag("onboarding_screen")) {
        when (state.currentStep) {
            OnboardingStep.INPUT_NICKNAME -> InputNicknameScreen(
                nickname = state.nickname,
                onNicknameChange = { onEvent(OnboardingUiEvent.NicknameChanged(it)) },
                onNextClick = { onEvent(OnboardingUiEvent.NextClicked) },
                onBackClick = onBackClick,
            )
            OnboardingStep.CHOOSE_ROLE -> ChooseRoleScreen(
                nickname = state.nickname,
                selectedRole = state.selectedRole,
                onRoleSelect = {
                    onEvent(OnboardingUiEvent.RoleSelected(it))
                    // 역할 선택 화면에는 다음 버튼이 없어 선택 즉시 진행
                    onEvent(OnboardingUiEvent.NextClicked)
                },
                onBackClick = onBackClick,
            )
            OnboardingStep.ONBOARDING_MAIN -> OnboardingMainScreen(
                onCreateFamilyClick = { onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY)) },
                onInputFamilyCodeClick = { onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.JOIN_FAMILY)) },
                onBrowseClick = { onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.BROWSE)) },
                onBackClick = onBackClick,
            )
            OnboardingStep.INPUT_FAMILY_NAME -> InputFamilyNameScreen(
                familyName = state.familyName,
                onFamilyNameChange = { onEvent(OnboardingUiEvent.FamilyNameChanged(it)) },
                onNextClick = { onEvent(OnboardingUiEvent.NextClicked) },
                onBackClick = onBackClick,
            )
            OnboardingStep.GENERATE_FAMILY_CODE_LOADING -> GenerateFamilyCodeLoadingScreen()
            OnboardingStep.GENERATE_FAMILY_CODE -> GenerateFamilyCodeScreen(
                familyCode = state.familyCode,
                onCancelClick = onBackClick,
                // TODO: 초대하기(공유) 동작 정해지면 연결
                onInviteClick = { onEvent(OnboardingUiEvent.NextClicked) },
                onBackClick = onBackClick,
            )
            OnboardingStep.INPUT_FAMILY_CODE -> InputFamilyCodeScreen(
                familyCode = state.inputFamilyCode,
                onFamilyCodeChange = { onEvent(OnboardingUiEvent.InputFamilyCodeChanged(it)) },
                onCancelClick = onBackClick,
                // TODO: 가족코드 확인/참여 동작 정해지면 연결
                onInviteClick = { onEvent(OnboardingUiEvent.NextClicked) },
                onBackClick = onBackClick,
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(state = OnboardingUiState(), onEvent = {})
}
