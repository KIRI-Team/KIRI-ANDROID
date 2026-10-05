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
import com.kiri.uikit.color.KiriColor
import com.kiri.feature.onboarding.presentation.component.OnboardingInputBox
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar

private val nicknameGuides = listOf(
    "한글/영어/숫자 혼합 가능",
    "최소 2자 ~ 최대 10자 이내",
    "특수 문자 입력 불가능",
)

@Composable
fun InputNicknameScreen(
    nickname: String,
    onNicknameChange: (String) -> Unit,
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
            .testTag("input_nickname_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            KiriTitle(
                title = "끼리에서 사용할\n닉네임을 설정해주세요.",
                subtitle = "가족 안에서의 역할을 선택해주세요!",
            )
            Spacer(Modifier.height(50.dp))
            OnboardingInputBox(
                value = nickname,
                onValueChange = { onNicknameChange(it.take(NicknameValidator.MAX_LENGTH)) },
                guides = nicknameGuides,
                placeholder = "이름을 입력하세요.",
            )
        }
        KiriButton(
            onClick = onNextClick,
            enabled = NicknameValidator.isValid(nickname),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun InputNicknameScreenPreview() {
    InputNicknameScreen(nickname = "", onNicknameChange = {}, onNextClick = {}, onBackClick = {})
}
