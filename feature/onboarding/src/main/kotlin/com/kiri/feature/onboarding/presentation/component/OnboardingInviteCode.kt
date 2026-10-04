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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.onboarding.R
import com.kiri.uikit.color.KiriColor

@Composable
fun OnboardingInviteCode(
    code: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KiriColor.Grayscale700)
            .clickable(onClickLabel = "가족코드 복사", role = Role.Button, onClick = onCopyClick)
            .padding(10.dp)
            .testTag("onboarding_invite_code"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "링크코드",
                style = TextStyle(
                    color = KiriColor.Grayscale400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = code,
                    style = TextStyle(
                        color = KiriColor.TextStrong,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.01).em,
                    ),
                )
                Image(
                    painter = painterResource(R.drawable.ic_onboarding_copy),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun OnboardingInviteCodePreview() {
    OnboardingInviteCode(code = "NWTKYJ", onCopyClick = {})
}
