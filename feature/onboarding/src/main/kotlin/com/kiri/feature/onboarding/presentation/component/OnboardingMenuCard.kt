package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.onboarding.R
import com.kiri.uikit.color.KiriColor

@Composable
fun OnboardingMenuCard(
    title: String,
    description: String,
    // 왼쪽 원 안의 아이콘 (만들기=Send, 코드 입력=Message, 둘러보기=Home)
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(87.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(onboardingCardBackground(pressed))
            // 눌림 피드백은 Pressed 배경색으로 표현해서 기본 리플은 끈다
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(KiriColor.Bg.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = KiriColor.Text.Strong,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 27.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Text(
                text = description,
                style = TextStyle(
                    color = KiriColor.Text.Tertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_onboarding_arrow_right),
            contentDescription = null,
        )
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun OnboardingMenuCardPreview() {
    OnboardingMenuCard(
        title = "가족공간 만들러 가기",
        description = "지금 바로 가족들을 초대해주세요!",
        icon = R.drawable.ic_onboarding_send,
        onClick = {},
    )
}
