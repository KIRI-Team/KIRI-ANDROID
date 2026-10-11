package com.kiri.feature.mypage.presentation.main.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.uikit.color.KiriColor

@Composable
fun MypageProfileRow(
    profile: MypageProfile,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // TODO: 프로필 이미지가 생기면 사용자 이미지로 교체 (현재는 기본 프로필 그림)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(KiriColor.Bg.Tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(painter = painterResource(R.drawable.ic_mypage_profile_default), contentDescription = null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = profile.nickname,
                    style = TextStyle(
                        color = KiriColor.Text.Strong,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.04).em,
                    ),
                )
                Text(
                    text = "${profile.familyName} I ${profile.role}",
                    style = TextStyle(
                        color = KiriColor.Text.Secondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.04).em,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onEditClick)
                .testTag("mypage_profile_edit"),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(R.drawable.ic_mypage_edit), contentDescription = "프로필 수정")
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun MypageProfileRowPreview() {
    MypageProfileRow(profile = MypageProfile.Sample, onEditClick = {})
}
