package com.kiri.feature.mypage.presentation.address

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.presentation.common.MypageInputBox
import com.kiri.uikit.color.KiriColor

// 주소 검색에서 보여주는 입력 예시 (설명, 예시)
private val searchExamples = listOf(
    "도로명 + 건물번호" to "남대문로 9길 40",
    "지역명 + 번지" to "중구 다동 155",
    "지역명 + 건물번호" to "분당 주공",
)

// 우편번호 찾기
@Composable
internal fun AddressSearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        MypageInputBox(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "도로명, 건물명, 지번 입력",
            testTag = "address_search_input",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            trailingIcon = {
                Image(
                    painter = painterResource(R.drawable.ic_mypage_search),
                    contentDescription = "검색",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSearch)
                        .testTag("address_search_button"),
                )
            },
        )
        Spacer(Modifier.height(36.dp))
        Text(
            text = "입력예시",
            style = TextStyle(
                color = KiriColor.Text.Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 27.sp,
                letterSpacing = (-0.04).em,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            searchExamples.forEach { (label, example) ->
                Column {
                    Text(
                        text = label,
                        style = TextStyle(
                            color = KiriColor.Text.Tertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                    Text(
                        text = example,
                        style = TextStyle(
                            color = KiriColor.Text.Strong,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun AddressSearchContentPreview() {
    AddressSearchContent(query = "", onQueryChange = {}, onSearch = {})
}
