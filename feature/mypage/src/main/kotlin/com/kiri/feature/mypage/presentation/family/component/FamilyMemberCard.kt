package com.kiri.feature.mypage.presentation.family.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kiri.uikit.color.KiriColor

// KIRI/Member Settings Card: 역할과 닉네임. 내 카드에는 닉네임 수정 아이콘이 붙는다
@Composable
fun FamilyMemberCard(
    member: FamilyMember,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(73.dp)
            .background(KiriColor.Bg.Secondary, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "역할 I ${member.role}",
                maxLines = 1,
                style = TextStyle(
                    color = KiriColor.Text.Secondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 23.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = member.nickname,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = KiriColor.Text.Strong,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 27.sp,
                        letterSpacing = (-0.04).em,
                    ),
                )
                if (member.isMe) {
                    Image(
                        painter = painterResource(R.drawable.ic_mypage_edit_24),
                        contentDescription = "닉네임 수정",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onEditClick)
                            .testTag("family_member_nickname_edit"),
                    )
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun FamilyMemberCardPreview() {
    FamilyMemberCard(member = FamilyMember(role = "딸", nickname = "쥬123", isMe = true), onEditClick = {})
}
