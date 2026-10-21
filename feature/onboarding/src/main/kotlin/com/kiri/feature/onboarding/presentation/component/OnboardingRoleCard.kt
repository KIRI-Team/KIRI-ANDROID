package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.kiri.uikit.R as UiR
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
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) KiriColor.Bg.GreenSoft else KiriColor.Bg.Primary
    val character = when (koreanName) {
        "엄마" -> UiR.drawable.kiri_avatar_mother
        "아빠" -> UiR.drawable.kiri_avatar_father
        "아들" -> UiR.drawable.kiri_avatar_son
        else -> UiR.drawable.kiri_avatar_daughter
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(character), contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                text = koreanName,
                textAlign = TextAlign.Center,
                style = com.kiri.uikit.KiriTypography.Value.copy(lineHeight = 28.sp),
                color = if (selected) KiriColor.Text.Accent else KiriColor.Text.Disabled,
            )
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun OnboardingRoleCardPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OnboardingRoleCard(koreanName = "엄마", selected = false, onClick = {})
        OnboardingRoleCard(koreanName = "아빠", selected = true, onClick = {})
    }
}
