package com.kiri.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.onboarding.presentation.component.OnboardingLoadingIndicator
import com.kiri.feature.onboarding.presentation.component.OnboardingTitle
import com.kiri.feature.onboarding.presentation.component.OnboardingTopBar
import com.kiri.uikit.color.KiriColor

@Composable
fun GenerateFamilyCodeLoadingScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .testTag("generate_family_code_loading_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 코드 생성 중에는 뒤로가기 버튼을 비활성화한다
        OnboardingTopBar(onBackClick = {}, enabled = false)
        Spacer(Modifier.height(154.dp))
        OnboardingTitle(
            title = "가족코드를\n생성하고 있어요!",
            subtitle = "조금만 기다려주세요",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(44.dp))
        OnboardingLoadingIndicator()
    }
}

@Preview(showSystemUi = true)
@Composable
private fun GenerateFamilyCodeLoadingScreenPreview() {
    GenerateFamilyCodeLoadingScreen()
}
