package com.kiri.feature.mypage.presentation.common

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.model.common.MypageSection
import com.kiri.uikit.color.KiriColor

// 섹션 제목과 그 아래 둥근 메뉴 묶음
@Composable
fun MypageMenuGroup(
    title: String,
    menus: List<MypageMenu>,
    onMenuClick: (MypageMenu) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = KiriColor.Text.Strong,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.04).em,
            ),
        )
        MypageMenuCard {
            menus.forEach { menu ->
                MypageMenuRow(
                    title = menu.title,
                    onClick = { onMenuClick(menu) },
                    testTag = "mypage_menu_${menu.name.lowercase()}",
                )
            }
        }
    }
}

// 둥근 메뉴 묶음 안의 한 줄. trailingText가 있으면 화살표 대신 글자를 보여주고, onClick이 없으면 눌리지 않는다
@Composable
internal fun MypageMenuRow(
    title: String,
    testTag: String,
    onClick: (() -> Unit)?,
    trailingText: String? = null,
    titleColor: Color = KiriColor.Text.Primary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.04).em,
            ),
        )
        if (trailingText != null) {
            Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = trailingText,
                    style = TextStyle(
                        color = KiriColor.Text.Tertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.04).em,
                    ),
                )
            }
        } else {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                // Figma처럼 왼쪽 화살표를 좌우 반전해서 오른쪽 화살표로 보여준다
                Image(
                    painter = painterResource(R.drawable.ic_mypage_chevron),
                    contentDescription = null,
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                )
            }
        }
    }
}

// 메뉴 줄을 담는 둥근 묶음 (제목 없음)
@Composable
internal fun MypageMenuCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KiriColor.Bg.Secondary),
    ) {
        content()
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun MypageMenuGroupPreview() {
    MypageMenuGroup(
        title = MypageSection.FAMILY.title,
        menus = MypageMenu.entries.filter { it.section == MypageSection.FAMILY },
        onMenuClick = {},
    )
}
