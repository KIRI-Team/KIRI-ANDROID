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
    // KIRI/Selection Card (Type=Role): 선택 여부에 따라 배경만 바뀐다
    val containerColor = if (selected) KiriColor.Bg.Elevated else KiriColor.Bg.Tertiary

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
                style = roleTextStyle(fontSize = 20, lineHeight = 28),
            )
            Text(
                text = englishName,
                textAlign = TextAlign.Center,
                style = roleTextStyle(fontSize = 18, lineHeight = 27),
            )
        }
    }
}

private fun roleTextStyle(
    fontSize: Int,
    lineHeight: Int,
) = TextStyle(
    color = KiriColor.Text.Strong,
    fontSize = fontSize.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = lineHeight.sp,
    letterSpacing = (-0.04).em,
)

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun OnboardingRoleCardPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OnboardingRoleCard(koreanName = "엄마", englishName = "Mom", selected = false, onClick = {})
        OnboardingRoleCard(koreanName = "아빠", englishName = "Dad", selected = true, onClick = {})
    }
}
