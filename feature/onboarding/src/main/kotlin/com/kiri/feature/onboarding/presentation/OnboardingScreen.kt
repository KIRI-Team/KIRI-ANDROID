package com.kiri.feature.onboarding.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.onboarding.OnboardingPath
import com.kiri.feature.onboarding.OnboardingStep
import com.kiri.feature.onboarding.OnboardingViewModel
import com.kiri.feature.onboarding.model.OnboardingUiEffect
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import com.kiri.feature.onboarding.model.OnboardingUiState
import com.kiri.uikit.R as UikitR
import com.kiri.uikit.invite.createInviteShareIntent

@Composable
fun OnboardingRoute(
    onNavigateToMainFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
    onBrowseClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToMainFlow by rememberUpdatedState(onNavigateToMainFlow)
    val context = LocalContext.current
    // 공유 시트에서 돌아오면 결과와 관계없이 다음 동작을 진행한다
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onEvent(OnboardingUiEvent.InviteShared)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OnboardingUiEffect.NavigateToMainFlow -> currentOnNavigateToMainFlow()
                is OnboardingUiEffect.ShareInvite -> shareLauncher.launch(context.createInviteShareIntent(effect.invite.url, effect.invite.code))
                OnboardingUiEffect.ShowInviteError ->
                    Toast.makeText(context, UikitR.string.invite_create_error, Toast.LENGTH_SHORT).show()
                OnboardingUiEffect.ShowNicknameError ->
                    Toast.makeText(context, "닉네임을 저장하지 못했어요. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                OnboardingUiEffect.ShowRoleError ->
                    Toast.makeText(context, "역할을 저장하지 못했어요. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                OnboardingUiEffect.ShowCreateFamilyError ->
                    Toast.makeText(context, "가족 공간을 만들지 못했어요. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    OnboardingScreen(
        modifier = modifier,
        state = state,
        onEvent = viewModel::onEvent,
        onBrowseClick = onBrowseClick,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onBrowseClick: () -> Unit = {},
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
                violations = state.nicknameViolations,
                isDuplicated = state.isNicknameDuplicated,
                isSubmitting = state.isSubmitting,
            )
            OnboardingStep.CHOOSE_ROLE -> ChooseRoleScreen(
                nickname = state.nickname,
                selectedRole = state.selectedRole,
                onRoleSelect = { onEvent(OnboardingUiEvent.RoleSelected(it)) },
                onNextClick = { onEvent(OnboardingUiEvent.NextClicked) },
                onBackClick = onBackClick,
                isSubmitting = state.isSubmitting,
            )
            OnboardingStep.ONBOARDING_MAIN -> OnboardingMainScreen(
                onCreateFamilyClick = { onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.CREATE_FAMILY)) },
                onInputFamilyCodeClick = { onEvent(OnboardingUiEvent.PathSelected(OnboardingPath.JOIN_FAMILY)) },
                onBrowseClick = onBrowseClick,
                onBackClick = onBackClick,
            )
            OnboardingStep.INPUT_FAMILY_NAME -> InputFamilyNameScreen(
                familyName = state.familyName,
                onFamilyNameChange = { onEvent(OnboardingUiEvent.FamilyNameChanged(it)) },
                onNextClick = { onEvent(OnboardingUiEvent.NextClicked) },
                onBackClick = onBackClick,
                violations = state.familyNameViolations,
                isDuplicated = state.isFamilyNameDuplicated,
            )
            OnboardingStep.GENERATE_FAMILY_CODE_LOADING -> GenerateFamilyCodeLoadingScreen()
            OnboardingStep.GENERATE_FAMILY_CODE -> GenerateFamilyCodeScreen(
                familyCode = state.familyCode,
                onCancelClick = onBackClick,
                onInviteClick = { onEvent(OnboardingUiEvent.InviteClicked) },
                onBackClick = onBackClick,
            )
            OnboardingStep.INPUT_FAMILY_CODE -> InputFamilyCodeScreen(
                familyCode = state.inputFamilyCode,
                onFamilyCodeChange = { onEvent(OnboardingUiEvent.InputFamilyCodeChanged(it)) },
                onCancelClick = onBackClick,
                onInviteClick = { onEvent(OnboardingUiEvent.InviteClicked) },
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
