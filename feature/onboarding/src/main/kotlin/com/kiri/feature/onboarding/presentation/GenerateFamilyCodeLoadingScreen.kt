package com.kiri.feature.onboarding.presentation

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.kiri.uikit.component.KiriInviteCodeLoadingScreen

// 온보딩은 메인 화면의 Scaffold 밖이라 시스템 바 여백을 직접 둔다
@Composable
fun GenerateFamilyCodeLoadingScreen(
    modifier: Modifier = Modifier,
) {
    KiriInviteCodeLoadingScreen(
        modifier = modifier
            .systemBarsPadding()
            .testTag("generate_family_code_loading_screen"),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun GenerateFamilyCodeLoadingScreenPreview() {
    GenerateFamilyCodeLoadingScreen()
}
