package com.kiri.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.onboarding.NicknameValidator
import com.kiri.feature.onboarding.presentation.component.OnboardingColors
import com.kiri.feature.onboarding.presentation.component.OnboardingInputBox
import com.kiri.feature.onboarding.presentation.component.OnboardingNextButton
import com.kiri.feature.onboarding.presentation.component.OnboardingTitle
import com.kiri.feature.onboarding.presentation.component.OnboardingTopBar

private val nicknameGuides = listOf(
    "한글/영어/숫자 혼합 가능",
    "최소 2자 ~ 최대 10자 이내",
    "특수 문자 입력 불가능",
)

@Composable
fun OnboardingScreen(
    onNextClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    var nickname by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(OnboardingColors.Background)
            .systemBarsPadding()
            .imePadding()
            .testTag("onboarding_screen"),
    ) {
        OnboardingTopBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            OnboardingTitle(
                title = "끼리에서 사용할\n닉네임을 설정해주세요.",
                subtitle = "가족 안에서의 역할을 선택해주세요!",
            )
            Spacer(Modifier.height(50.dp))
            OnboardingInputBox(
                value = nickname,
                onValueChange = { nickname = it.take(NicknameValidator.MAX_LENGTH) },
                guides = nicknameGuides,
            )
        }
        OnboardingNextButton(
            onClick = onNextClick,
            enabled = NicknameValidator.isValid(nickname),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen()
}
