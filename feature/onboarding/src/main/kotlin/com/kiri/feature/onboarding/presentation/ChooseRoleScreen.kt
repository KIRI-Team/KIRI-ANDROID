package com.kiri.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.uikit.color.KiriColor
import com.kiri.feature.onboarding.presentation.component.OnboardingRoleCard
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar

@Composable
fun ChooseRoleScreen(
    nickname: String,
    selectedRole: FamilyRole?,
    onRoleSelect: (FamilyRole) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // 서버에 저장하는 동안 다음 버튼을 막는다
    isSubmitting: Boolean = false,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .testTag("choose_role_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick)
        Spacer(Modifier.height(24.dp))
        KiriTitle(
            title = "${nickname}님의\n역할을 선택해주세요.",
            subtitle = "가족 안에서의 역할을 선택해주세요!",
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(56.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FamilyRole.entries.chunked(2).forEach { rowRoles ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowRoles.forEach { role ->
                        OnboardingRoleCard(
                            koreanName = role.koreanName,
                            selected = role == selectedRole,
                            onClick = { onRoleSelect(role) },
                        )
                    }
                }
            }
        }
        // 역할을 고르면 다음 버튼이 나타나고, 누를 때까지 다른 역할로 바꿀 수 있다
        if (selectedRole != null) {
            KiriButton(
                onClick = onNextClick,
                enabled = !isSubmitting,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ChooseRoleScreenPreview() {
    ChooseRoleScreen(
        nickname = "주연주",
        selectedRole = FamilyRole.DAD,
        onRoleSelect = {},
        onNextClick = {},
        onBackClick = {},
    )
}
