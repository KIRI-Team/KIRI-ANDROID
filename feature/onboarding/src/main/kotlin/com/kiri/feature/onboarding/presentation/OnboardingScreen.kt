package com.kiri.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun OnboardingScreen(
    onNextClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    var nickname by rememberSaveable { mutableStateOf("") }

    InputNicknameScreen(
        nickname = nickname,
        onNicknameChange = { nickname = it },
        onNextClick = onNextClick,
        onBackClick = onBackClick,
        modifier = Modifier.testTag("onboarding_screen"),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen()
}
