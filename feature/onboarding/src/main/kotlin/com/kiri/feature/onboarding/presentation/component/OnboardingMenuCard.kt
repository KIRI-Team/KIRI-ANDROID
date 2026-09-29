package com.kiri.feature.onboarding.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun OnboardingMenuCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(87.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(OnboardingColors.Grayscale700)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(OnboardingColors.Grayscale800, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_onboarding_envelope),
                contentDescription = null,
                // Figma에서 봉투 아이콘은 원의 중심보다 약간 위에 배치되어 있다
                modifier = Modifier.offset(y = (-1.4).dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = OnboardingColors.Grayscale100,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.02).em,
                ),
            )
            Text(
                text = description,
                style = TextStyle(
                    color = OnboardingColors.Grayscale500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.02).em,
                ),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_onboarding_arrow_right),
            contentDescription = null,
        )
    }
}

@Preview(backgroundColor = 0xFF242426, showBackground = true)
@Composable
private fun OnboardingMenuCardPreview() {
    OnboardingMenuCard(
        title = "가족공간 만들러 가기",
        description = "지금 바로 가족들을 초대해주세요!",
        onClick = {},
    )
}
