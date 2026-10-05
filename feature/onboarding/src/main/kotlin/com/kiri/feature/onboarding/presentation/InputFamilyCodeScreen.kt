package com.kiri.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.onboarding.presentation.component.OnboardingButtonTone
import com.kiri.feature.onboarding.presentation.component.OnboardingCodeInput
import com.kiri.feature.onboarding.presentation.component.OnboardingNextButton
import com.kiri.feature.onboarding.presentation.component.OnboardingTitle
import com.kiri.feature.onboarding.presentation.component.OnboardingTopBar
import com.kiri.uikit.color.KiriColor

@Composable
fun InputFamilyCodeScreen(
    familyCode: String,
    onFamilyCodeChange: (String) -> Unit,
    onCancelClick: () -> Unit,
    onInviteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .imePadding()
            .testTag("input_family_code_screen"),
    ) {
        OnboardingTopBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            OnboardingTitle(
                title = "가족코드를\n입력해주세요!",
                subtitle = "총 6자리 코드 입력을 해주셔야해요",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(68.dp))
            OnboardingCodeInput(
                code = familyCode,
                onCodeChange = onFamilyCodeChange,
            )
        }
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingNextButton(
                onClick = onCancelClick,
                text = "취소",
                tone = OnboardingButtonTone.Neutral,
                modifier = Modifier.weight(1f),
            )
            OnboardingNextButton(
                onClick = onInviteClick,
                text = "초대하기",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun InputFamilyCodeScreenPreview() {
    InputFamilyCodeScreen(familyCode = "", onFamilyCodeChange = {}, onCancelClick = {}, onInviteClick = {}, onBackClick = {})
}
