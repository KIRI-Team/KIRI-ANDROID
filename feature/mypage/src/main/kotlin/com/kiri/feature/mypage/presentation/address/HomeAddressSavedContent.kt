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

// 등록한 주소 보기
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
                withStyle(SpanStyle(color = KiriColor.Text.Accent)) { append(familyName) }
                append("님의 공간")
            },
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = address,
            modifier = Modifier
                .fillMaxWidth()
                .background(KiriColor.Bg.Primary, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .testTag("home_address_saved"),
            style = TextStyle(
                color = KiriColor.Text.Strong,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 28.sp,
                letterSpacing = (-0.04).em,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onEditClick)
                .testTag("home_address_edit"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(painter = painterResource(R.drawable.ic_mypage_edit_24), contentDescription = null)
            Text(
                text = "수정하기",
                style = TextStyle(
                    color = KiriColor.Text.Tertiary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun HomeAddressSavedContentPreview() {
    HomeAddressSavedContent(familyName = "끼리네가족", address = "강남구 영동대로 234, 710-1403호", onEditClick = {})
}
