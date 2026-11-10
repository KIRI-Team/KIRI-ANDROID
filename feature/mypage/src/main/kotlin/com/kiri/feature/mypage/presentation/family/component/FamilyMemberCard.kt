package com.kiri.feature.mypage.presentation.family.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.model.family.FamilyMember
import com.kiri.uikit.R as UiR
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriAsyncImage

// Figma 10298:38243 가족 구성원 카드: 캐릭터, 닉네임, 역할. 내 카드에는 닉네임 수정 아이콘이 붙는다
@Composable
fun FamilyMemberCard(
    member: FamilyMember,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KiriColor.Bg.Primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("family_member_${member.id.ifBlank { member.nickname }}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 캐릭터 40 칸(배경 Bg.Secondary, 모서리 4, 안쪽 여백 2)에 32 크기 그림.
            // 서버 캐릭터 이미지가 있으면 그것을, 없거나 불러오는 중·실패면 역할별 기본 캐릭터를 보여준다
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(KiriColor.Bg.Secondary, RoundedCornerShape(4.dp))
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                KiriAsyncImage(
                    model = member.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit,
                ) {
                    Image(
                        painter = painterResource(roleAvatar(member.role)),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Column {
                Text(
                    text = member.nickname,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = KiriColor.Text.Strong,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 26.sp,
                        letterSpacing = (-0.04).em,
                    ),
                )
                if (member.role.isNotBlank()) {
                    Text(
                        text = member.role,
                        maxLines = 1,
                        style = TextStyle(
                            color = KiriColor.Text.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                }
            }
        }
        if (member.isMe) {
            Image(
                painter = painterResource(R.drawable.ic_mypage_edit),
                contentDescription = "닉네임 수정",
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onEditClick)
                    .testTag("family_member_nickname_edit"),
            )
        }
    }
}

// 역할별 기본 캐릭터. 역할을 정하지 않았으면 기본 구성원 그림
private fun roleAvatar(role: String): Int = when (role) {
    "엄마" -> UiR.drawable.kiri_avatar_mother
    "아빠" -> UiR.drawable.kiri_avatar_father
    "딸" -> UiR.drawable.kiri_avatar_daughter
    "아들" -> UiR.drawable.kiri_avatar_son
    else -> UiR.drawable.kiri_member_avatar
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun FamilyMemberCardPreview() {
    FamilyMemberCard(member = FamilyMember(role = "아빠", nickname = "아빠다", isMe = true), onEditClick = {})
}
