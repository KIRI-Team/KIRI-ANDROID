package com.kiri.feature.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.auth.AuthStep
import com.kiri.feature.auth.AuthViewModel
import com.kiri.feature.auth.model.AuthUiEffect
import com.kiri.feature.auth.model.AuthUiEvent
import com.kiri.feature.auth.model.AuthUiState

@Composable
fun AuthRoute(
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToOnboarding by rememberUpdatedState(onNavigateToOnboarding)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthUiEffect.NavigateToOnboarding -> currentOnNavigateToOnboarding()
            }
        }
    }

    AuthScreen(
        modifier = modifier,
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBackClick = { onEvent(AuthUiEvent.BackClicked) }

    // 첫 단계(로그인)에서는 비활성화해서 시스템 뒤로가기로 앱을 종료할 수 있게 한다
    BackHandler(enabled = state.currentStep != AuthStep.LOGIN, onBack = onBackClick)

    // 첫 화면 애니메이션은 한 번만 보여준다 (약관동의에서 돌아오거나 화면을 돌려도 다시 재생하지 않는다)
    var introPlayed by rememberSaveable { mutableStateOf(false) }

    Box(modifier.testTag("auth_screen")) {
        when (state.currentStep) {
            AuthStep.LOGIN -> LoginScreen(
                onLoginClick = { onEvent(AuthUiEvent.LoginClicked(it)) },
                playIntro = !introPlayed,
                onIntroFinished = { introPlayed = true },
            )
            AuthStep.TERMS_AGREEMENT -> TermsAgreementScreen(
                agreedTerms = state.agreedTerms,
                allAgreed = state.allAgreed,
                canContinue = state.canContinue,
                onAllToggle = { onEvent(AuthUiEvent.AllTermsToggled) },
                onTermToggle = { onEvent(AuthUiEvent.TermToggled(it)) },
                onTermDetailClick = { onEvent(AuthUiEvent.TermDetailClicked(it)) },
                onAgreeClick = { onEvent(AuthUiEvent.AgreeClicked) },
                onBackClick = onBackClick,
                viewingTerm = state.viewingTerm,
                onTermDetailDismiss = { onEvent(AuthUiEvent.TermDetailDismissed) },
                onTermDetailAgree = { onEvent(AuthUiEvent.TermDetailAgreed(it)) },
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AuthScreenPreview() {
    AuthScreen(state = AuthUiState(), onEvent = {})
}
