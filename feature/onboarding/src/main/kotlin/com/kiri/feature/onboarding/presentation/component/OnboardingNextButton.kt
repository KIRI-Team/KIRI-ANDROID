package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor

@Composable
fun OnboardingNextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "다음",
) {
    // TODO: Figma에는 비활성 상태만 있어 활성 상태 색상은 임시값. 디자인 확정 후 교체
    val containerColor = if (enabled) KiriColor.Grayscale100 else KiriColor.Grayscale800
    val contentColor = if (enabled) KiriColor.Background else KiriColor.Grayscale400

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("onboarding_next_button"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 1.45.em,
                letterSpacing = (-0.01).em,
            ),
        )
    }
}

@Preview(backgroundColor = 0xFF242426, showBackground = true)
@Composable
private fun OnboardingNextButtonPreview() {
    OnboardingNextButton(onClick = {}, enabled = false)
}
