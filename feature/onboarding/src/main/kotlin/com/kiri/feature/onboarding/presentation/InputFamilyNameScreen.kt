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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.onboarding.NicknameValidator
import com.kiri.feature.onboarding.presentation.component.OnboardingInputBox
import com.kiri.feature.onboarding.presentation.component.OnboardingNextButton
import com.kiri.feature.onboarding.presentation.component.OnboardingTitle
import com.kiri.feature.onboarding.presentation.component.OnboardingTopBar
import com.kiri.uikit.color.KiriColor

private val familyNameGuides = listOf(
    "한글/영어/숫자 혼합 가능",
    "최소 2자 ~ 최대 10자 이내",
    "특수 문자 입력 불가능",
)

@Composable
fun InputFamilyNameScreen(
    familyName: String,
    onFamilyNameChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .imePadding()
            .testTag("input_family_name_screen"),
    ) {
        OnboardingTopBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            OnboardingTitle(
                title = "끼리에서 사용할\n가족명을 작성해주세요",
                subtitle = "우리 가족을 뭐라고 불러드릴까요?",
            )
            Spacer(Modifier.height(50.dp))
            // 가족명 규칙은 닉네임 규칙과 동일하다
            OnboardingInputBox(
                value = familyName,
                onValueChange = { onFamilyNameChange(it.take(NicknameValidator.MAX_LENGTH)) },
                guides = familyNameGuides,
                placeholder = "가족명을 입력하세요.",
            )
        }
        OnboardingNextButton(
            onClick = onNextClick,
            enabled = NicknameValidator.isValid(familyName),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun InputFamilyNameScreenPreview() {
    InputFamilyNameScreen(familyName = "끼리네가족", onFamilyNameChange = {}, onNextClick = {}, onBackClick = {})
}
