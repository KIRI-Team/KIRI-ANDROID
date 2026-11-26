package com.kiri.feature.mypage.presentation.address

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kiri.feature.mypage.model.address.HomeAddress
import com.kiri.feature.mypage.presentation.address.component.HomeAddressTitle
import com.kiri.feature.mypage.presentation.common.MypageInputBox
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriButtonTone

// 주소 등록·수정. 우편번호와 주소는 우편번호 찾기로 채우고, 상세주소는 주소를 고른 뒤 입력한다
@Composable
internal fun HomeAddressFormContent(
    isEdit: Boolean,
    form: HomeAddress,
    onFindZipCodeClick: () -> Unit,
    onDetailAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        HomeAddressTitle(title = AnnotatedString(if (isEdit) "주소를 수정해주세요" else "아직 작성된 주소가 없어요!"))
        Spacer(Modifier.height(if (isEdit) 24.dp else 48.dp))
        Text(
            text = if (form.roadAddress.isEmpty()) "우리 집 주소 작성하기" else "우리 집 주소",
            style = TextStyle(
                color = KiriColor.Text.Strong,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.04).em,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MypageInputBox(
                    value = form.zipCode,
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    placeholder = "주소를 입력해주세요",
                    enabled = form.zipCode.isNotEmpty(),
                    readOnly = true,
                    testTag = "home_address_zip_code",
                )
                // 글꼴에 따라 글자가 두 줄로 넘치지 않도록 글자 너비에 맞추되 디자인 너비(113dp)보다 좁아지지 않게 한다
                KiriButton(
                    onClick = onFindZipCodeClick,
                    modifier = Modifier
                        .widthIn(min = 113.dp)
                        .width(IntrinsicSize.Max),
                    text = "우편번호 찾기",
                    tone = KiriButtonTone.Neutral,
                )
            }
            MypageInputBox(
                value = form.roadAddress,
                onValueChange = {},
                placeholder = "주소를 입력해주세요",
                enabled = form.roadAddress.isNotEmpty(),
                readOnly = true,
                testTag = "home_address_road",
            )
            MypageInputBox(
                value = form.detailAddress,
                onValueChange = onDetailAddressChange,
                placeholder = "상세주소를 입력해주세요",
                enabled = form.roadAddress.isNotEmpty(),
                testTag = "home_address_detail",
            )
        }
    }
}

@Preview(backgroundColor = 0xFF1A1A1A, showBackground = true)
@Composable
private fun HomeAddressFormContentPreview() {
    HomeAddressFormContent(
        isEdit = false,
        form = HomeAddress(zipCode = "06134", roadAddress = "서울 서초구 강남대로 143", detailAddress = "810-603"),
        onFindZipCodeClick = {},
        onDetailAddressChange = {},
    )
}
