package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor

@Composable
fun OnboardingTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = KiriColor.Title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 1.4.em,
                letterSpacing = (-0.02).em,
            ),
        )
        Text(
            text = subtitle,
            style = TextStyle(
                color = KiriColor.Grayscale600,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 1.4.em,
                letterSpacing = (-0.04).em,
            ),
        )
    }
}

@Preview(backgroundColor = 0xFF242426, showBackground = true)
@Composable
private fun OnboardingTitlePreview() {
    OnboardingTitle(
        title = "끼리에서 사용할\n닉네임을 설정해주세요.",
        subtitle = "가족 안에서의 역할을 선택해주세요!",
    )
}
