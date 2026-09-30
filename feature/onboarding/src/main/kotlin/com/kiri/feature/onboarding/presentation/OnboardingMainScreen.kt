package com.kiri.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.uikit.color.KiriColor
import com.kiri.feature.onboarding.presentation.component.OnboardingMenuCard
import com.kiri.feature.onboarding.presentation.component.OnboardingTitle
import com.kiri.feature.onboarding.presentation.component.OnboardingTopBar

@Composable
fun OnboardingMainScreen(
    onCreateFamilyClick: () -> Unit,
    onInputFamilyCodeClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Background)
            .systemBarsPadding()
            .testTag("onboarding_main_screen"),
    ) {
        OnboardingTopBar(onBackClick = onBackClick)
        Spacer(Modifier.height(24.dp))
        OnboardingTitle(
            title = "지금 바로 가족공간을\n만들어봐요!",
            subtitle = "관심가지고 있는 분야에 대해 알려주세요!",
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(58.dp))
        Column(
            modifier = Modifier.padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            OnboardingMenuCard(
                title = "가족공간 만들러 가기",
                description = "지금 바로 가족들을 초대해주세요!",
                onClick = onCreateFamilyClick,
            )
            OnboardingMenuCard(
                title = "가족코드 입력하기",
                description = "전달받은 가족코드를 입력해주세요!",
                onClick = onInputFamilyCodeClick,
            )
            OnboardingMenuCard(
                title = "먼저 둘러보러가기",
                description = "할 수 있는 미션을 먼저 살펴봐요!",
                onClick = onBrowseClick,
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingMainScreenPreview() {
    OnboardingMainScreen(
        onCreateFamilyClick = {},
        onInputFamilyCodeClick = {},
        onBrowseClick = {},
        onBackClick = {},
    )
}
