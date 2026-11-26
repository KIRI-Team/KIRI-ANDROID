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
import androidx.compose.ui.layout.ContentScale
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
import com.kiri.uikit.R as UiR
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
            // 내 캐릭터 (Handoff 9901:25866): 지름 48 원(Bg.Tertiary) 안에 역할별 회색 캐릭터를 27.2 크기로 둔다.
            // 역할을 정하지 않았으면 기본 프로필 그림을 둔다
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(KiriColor.Bg.Tertiary, CircleShape)
                    .testTag("mypage_profile_image"),
                contentAlignment = Alignment.Center,
            ) {
                val avatar = profileAvatar(profile.role)
                if (avatar == null) {
                    Image(painter = painterResource(R.drawable.ic_mypage_profile_default), contentDescription = null)
                } else {
                    Image(
                        painter = painterResource(avatar),
                        contentDescription = null,
                        modifier = Modifier.size(27.2.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
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
                    // 역할을 정하지 않았거나 불러오기 전이면 빈 칸 없이 있는 값만 잇는다
                    text = listOf(profile.familyName, profile.role).filter { it.isNotBlank() }.joinToString(" I "),
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

// 마이페이지 프로필의 역할별 회색 캐릭터. 역할을 정하지 않았으면 null
private fun profileAvatar(role: String): Int? = when (role) {
    "엄마" -> UiR.drawable.kiri_avatar_mother_off
    "아빠" -> UiR.drawable.kiri_avatar_father_off
    "딸" -> UiR.drawable.kiri_avatar_daughter_off
    "아들" -> UiR.drawable.kiri_avatar_son_off
    else -> null
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun MypageProfileRowPreview() {
    MypageProfileRow(profile = MypageProfile.Sample, onEditClick = {})
}
