package com.kiri.uikit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.R
import com.kiri.uikit.color.KiriColor

// KIRI/App Bar: 왼쪽 뒤로가기, 가운데 제목(선택), 오른쪽 영역(선택)
@Composable
fun KiriTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 52.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = KiriColor.Text.Strong,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 27.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, onClick = onBackClick)
                    .testTag("kiri_back_button"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_kiri_back_arrow),
                    contentDescription = "뒤로가기",
                    colorFilter = if (enabled) null else ColorFilter.tint(KiriColor.Icon.Disabled),
                )
            }
        }
        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                trailing()
            }
        }
    }
}

// 앱바 오른쪽 글자 버튼 (예: 등록하기). 비활성화면 Strong 색으로 보이고 눌리지 않는다
@Composable
fun KiriTopBarTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(width = 100.dp, height = 52.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("kiri_top_bar_text_button"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = if (enabled) KiriColor.Text.Accent else KiriColor.Text.Strong,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.04).em,
            ),
        )
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun KiriTopBarPreview() {
    KiriTopBar(onBackClick = {})
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun KiriTopBarWithTitlePreview() {
    KiriTopBar(
        onBackClick = {},
        title = "우리 집 위치",
        trailing = { KiriTopBarTextButton(text = "등록하기", onClick = {}) },
    )
}
