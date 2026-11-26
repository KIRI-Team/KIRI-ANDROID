package com.kiri.feature.mypage.presentation.address

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.presentation.address.component.HomeAddressTitle
import com.kiri.uikit.color.KiriColor

// Figma 10298:38339 (우리집위치/O) 등록한 주소 보기.
// 제목 "OO님의 공간", 안내 문구, 주소 칸(KIRI/Input 모양)과 그 안의 수정 아이콘. 주소 칸을 누르면 수정 화면을 연다
@Composable
internal fun HomeAddressSavedContent(
    familyName: String,
    address: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        HomeAddressTitle(
            title = buildAnnotatedString {
                // 가족 이름을 아직 못 불러왔거나 불러오지 못하면 "우리 가족" 으로 쓴다
                withStyle(SpanStyle(color = KiriColor.Text.Accent)) { append(familyName.ifBlank { "우리 가족" }) }
                append("님의 공간")
            },
        )
        Spacer(Modifier.height(24.dp))
        // KIRI/Input: 높이 54, 배경 #2C2C2B(Bg.Secondary), 모서리 8, 좌우 24·상하 18 여백, 글자 Title/Medium(16·Medium·24)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KiriColor.Bg.Secondary)
                .clickable(role = Role.Button, onClick = onEditClick)
                .padding(horizontal = 24.dp, vertical = 15.dp)
                .testTag("home_address_edit"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = address,
                modifier = Modifier.weight(1f).testTag("home_address_saved"),
                style = TextStyle(
                    color = KiriColor.Text.Secondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            // KIRI/Icon/Action/Edit (24, 비활성 색 #737473)
            Image(
                painter = painterResource(R.drawable.ic_mypage_edit_24),
                contentDescription = "주소 수정",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(KiriColor.Icon.Disabled),
            )
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun HomeAddressSavedContentPreview() {
    HomeAddressSavedContent(familyName = "끼리네가족", address = "강남구 영동대로 234, 710-1403호", onEditClick = {})
}
