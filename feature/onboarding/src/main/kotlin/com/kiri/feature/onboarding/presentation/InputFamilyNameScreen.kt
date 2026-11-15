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
import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.feature.onboarding.presentation.component.OnboardingInputBox
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar
import com.kiri.uikit.color.KiriColor

@Composable
fun InputFamilyNameScreen(
    familyName: String,
    onFamilyNameChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    violations: Set<NicknameRule> = emptySet(),
    isDuplicated: Boolean = false,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .imePadding()
            .testTag("input_family_name_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            KiriTitle(
                title = "끼리에서 사용할\n가족명을 작성해주세요",
                subtitle = "우리 가족을 뭐라고 불러드릴까요?",
            )
            Spacer(Modifier.height(49.dp))
            // 가족명 규칙은 닉네임 규칙과 동일하다
            OnboardingInputBox(
                value = familyName,
                onValueChange = onFamilyNameChange,
                guides = familyNameGuides(violations, isDuplicated),
                isError = violations.isNotEmpty() || isDuplicated,
                placeholder = "가족명을 입력하세요.",
            )
        }
        KiriButton(
            onClick = onNextClick,
            // 한 글자라도 입력하면 활성화하고, 규칙 확인은 누른 시점에 한다
            enabled = familyName.isNotEmpty(),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun InputFamilyNameScreenPreview() {
    InputFamilyNameScreen(familyName = "끼리네가족", onFamilyNameChange = {}, onNextClick = {}, onBackClick = {})
}
