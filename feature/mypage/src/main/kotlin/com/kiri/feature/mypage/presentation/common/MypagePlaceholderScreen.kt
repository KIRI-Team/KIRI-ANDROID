package com.kiri.feature.mypage.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriTopBar

// 디자인이 나오기 전까지 마이페이지 메뉴 화면이 함께 쓰는 자리표시자
@Composable
internal fun MypagePlaceholderScreen(
    title: String,
    onBackClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag(testTag),
    ) {
        KiriTopBar(onBackClick = onBackClick)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$title\n화면 준비 중",
                style = TextStyle(
                    color = KiriColor.Text.Secondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.04).em,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
