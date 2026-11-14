package com.kiri.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.kiri.feature.onboarding.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.uikit.color.KiriColor
import com.kiri.feature.onboarding.presentation.component.OnboardingInputBox
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar

@Composable
fun InputNicknameScreen(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    violations: Set<NicknameRule> = emptySet(),
    isDuplicated: Boolean = false,
    // 서버에 저장하는 동안 입력과 다음 버튼을 막는다
    isSubmitting: Boolean = false,
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
            Spacer(Modifier.height(49.dp))
            OnboardingInputBox(
                value = nickname,
                onValueChange = onNicknameChange,
                guides = nicknameGuides(violations, isDuplicated),
                isError = violations.isNotEmpty() || isDuplicated,
                placeholder = "이름을 입력하세요.",
                enabled = !isSubmitting,
                // Figma 닉네임 입력칸 오른쪽 화살표 (Handoff 9817:14123)
                trailingIcon = {
                    Image(painter = painterResource(R.drawable.ic_onboarding_arrow_right), contentDescription = null)
                },
            )
        }
        KiriButton(
            onClick = onNextClick,
            // 한 글자라도 입력하면 활성화하고, 규칙 확인은 누른 시점에 한다
            enabled = nickname.isNotEmpty() && !isSubmitting,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun InputNicknameScreenPreview() {
    InputNicknameScreen(nickname = "", onNicknameChange = {}, onNextClick = {}, onBackClick = {})
}
