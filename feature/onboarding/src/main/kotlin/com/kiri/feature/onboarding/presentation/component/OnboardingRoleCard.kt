package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor

@Composable
fun OnboardingRoleCard(
    koreanName: String,
    englishName: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) KiriColor.Grayscale100 else KiriColor.Grayscale700
    val contentColor = if (selected) KiriColor.Grayscale700 else KiriColor.Grayscale500
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = modifier
            .size(width = 146.dp, height = 140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = koreanName,
                textAlign = TextAlign.Center,
                style = roleTextStyle(contentColor, fontWeight, 24),
            )
            Text(
                text = englishName,
                textAlign = TextAlign.Center,
                style = roleTextStyle(contentColor, fontWeight, 18),
            )
        }
    }
}

private fun roleTextStyle(
    color: Color,
    fontWeight: FontWeight,
    fontSize: Int,
) = TextStyle(
    color = color,
    fontSize = fontSize.sp,
    fontWeight = fontWeight,
    lineHeight = 1.5.em,
    letterSpacing = (-0.01).em,
)

@Preview(backgroundColor = 0xFF242426, showBackground = true)
@Composable
private fun OnboardingRoleCardPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OnboardingRoleCard(koreanName = "엄마", englishName = "Mom", selected = false, onClick = {})
        OnboardingRoleCard(koreanName = "아빠", englishName = "Dad", selected = true, onClick = {})
    }
}
