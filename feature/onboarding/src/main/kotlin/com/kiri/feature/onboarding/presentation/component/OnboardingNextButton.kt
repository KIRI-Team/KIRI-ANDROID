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

enum class OnboardingButtonTone { Accent, Neutral }

@Composable
fun OnboardingNextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "다음",
    tone: OnboardingButtonTone = OnboardingButtonTone.Accent,
) {
    val containerColor = when {
        !enabled -> KiriColor.Grayscale800
        tone == OnboardingButtonTone.Accent -> KiriColor.Accent
        else -> KiriColor.Grayscale800
    }
    val contentColor = when {
        !enabled -> KiriColor.Grayscale400
        tone == OnboardingButtonTone.Accent -> KiriColor.Background
        else -> KiriColor.TextPrimary
    }
    val textStyle = when (tone) {
        OnboardingButtonTone.Accent -> TextStyle(
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 1.45.em,
            letterSpacing = (-0.01).em,
        )
        OnboardingButtonTone.Neutral -> TextStyle(
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp,
            letterSpacing = (-0.04).em,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("onboarding_next_button"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = textStyle,
        )
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun OnboardingNextButtonPreview() {
    OnboardingNextButton(onClick = {}, enabled = false)
}
