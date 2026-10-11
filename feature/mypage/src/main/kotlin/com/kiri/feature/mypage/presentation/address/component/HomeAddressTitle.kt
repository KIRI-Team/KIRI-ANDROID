package com.kiri.feature.mypage.presentation.address.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.uikit.color.KiriColor

// 우리집 주소 화면 위쪽 제목과 안내 문구. 제목 일부를 강조색으로 칠할 수 있도록 AnnotatedString을 받는다
@Composable
fun HomeAddressTitle(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = KiriColor.Text.Strong,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 1.4.em,
                letterSpacing = (-0.02).em,
            ),
        )
        Text(
            text = "주소는 더 정확한 AI 미션 제공을 위해 사용됩니다",
            style = TextStyle(
                color = KiriColor.Text.Tertiary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 1.4.em,
                letterSpacing = (-0.04).em,
            ),
        )
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun HomeAddressTitlePreview() {
    HomeAddressTitle(title = AnnotatedString("아직 작성된 주소가 없어요!"))
}
